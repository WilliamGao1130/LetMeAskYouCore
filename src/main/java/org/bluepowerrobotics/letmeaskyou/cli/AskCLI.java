package org.bluepowerrobotics.letmeaskyou.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bluepowerrobotics.converter.core.ChatChunk;
import org.bluepowerrobotics.converter.core.ChatMessage;
import org.bluepowerrobotics.converter.core.ChatModel;
import org.bluepowerrobotics.converter.core.ChatRequest;
import org.bluepowerrobotics.converter.core.ChatStreamListener;
import org.bluepowerrobotics.converter.core.ToolCall;
import org.bluepowerrobotics.letmeaskyou.core.adapter.AdapterManager;
import org.bluepowerrobotics.letmeaskyou.core.chat.ChatEngine;
import org.bluepowerrobotics.letmeaskyou.core.config.ModelConfig;
import org.bluepowerrobotics.letmeaskyou.core.conversation.Conversation;
import org.bluepowerrobotics.letmeaskyou.core.conversation.Message;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Content;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.PictureFile;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Reasoning;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.RichText;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.TextFile;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.ToolCallContent;
import org.bluepowerrobotics.letmeaskyou.core.toolcall.CurrentTimeTool;
import org.bluepowerrobotics.letmeaskyou.core.toolcall.FetchUrl;
import org.bluepowerrobotics.letmeaskyou.core.toolcall.ToolsManager;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仿照原 “那我问你 / ask-ai” 脚本的 Java CLI，基于 LetMeAskYouCore 实现，
 * 额外支持：思考链展示、工具调用与结果展示、会话持久化、多提供商。
 */
public final class AskCLI {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DEFAULT_PROMPT =
            "你是一个智能助手，旨在回答用户的问题。请根据用户的提问提供准确和有用的答案。";
    private static final String DEFAULT_MODEL = "deepseek-v4-flash";
    private static final String DEFAULT_URL = "https://api.deepseek.com";
    private static final String FILE_PREFIX = "[文件: ";

    private static final String GRAY = "\u001b[90m";
    private static final String CYAN = "\u001b[36m";
    private static final String YELLOW = "\u001b[33m";
    private static final String RESET = "\u001b[0m";

    private boolean color = true;

    public static void main(String[] args) throws Exception {
        forceUtf8Output();
        int code = new AskCLI().run(args);
        if (code != 0) {
            System.exit(code);
        }
    }

    /** 强制 stdout/stderr 使用 UTF-8，避免 JVM 在非 UTF-8 locale 下乱码。 */
    private static void forceUtf8Output() {
        try {
            System.setOut(new PrintStream(
                    new FileOutputStream(FileDescriptor.out), true, "UTF-8"));
            System.setErr(new PrintStream(
                    new FileOutputStream(FileDescriptor.err), true, "UTF-8"));
        } catch (IOException ignored) {
            // 保持默认输出
        }
    }

    public int run(String[] args) throws Exception {
        Options opts = new Options();
        if (!opts.parse(args)) {
            usage();
            return 2;
        }
        if (opts.help) {
            usage();
            return 0;
        }
        color = !opts.noColor;

        File dataDir = opts.dataDir;
        if (!dataDir.isDirectory() && !dataDir.mkdirs()) {
            System.err.println("无法创建数据目录: " + dataDir);
            return 1;
        }

        String session = resolveSession(dataDir, opts.session);
        File historyFile = new File(dataDir, session + ".json");

        Conversation conversation = historyFile.isFile()
                ? load(historyFile)
                : newConversation(session, effectivePrompt(opts.prompt));
        boolean dirty = false;

        if (opts.clearHistory) {
            if (historyFile.exists() && !historyFile.delete()) {
                System.err.println("删除历史记录失败: " + historyFile);
                return 1;
            }
            System.out.println("已清除会话 " + session + " 的历史记录。");
            conversation = newConversation(session, effectivePrompt(opts.prompt));
            dirty = true;
        }

        if (opts.prompt != null) {
            setPrompt(conversation, opts.prompt);
            dirty = true;
        }
        if (opts.addFile != null) {
            addFileMessage(conversation, opts.addFile);
            dirty = true;
        }
        if (opts.clearFile) {
            dirty |= clearFileMessages(conversation);
        }
        if (opts.showHistory) {
            printHistory(conversation);
        }

        if (opts.question != null) {
            ask(historyFile, conversation, opts);
        } else if (dirty && !opts.clearHistory) {
            save(historyFile, conversation);
        }
        return 0;
    }

    private void ask(File historyFile, Conversation conversation, Options opts)
            throws Exception {
        Message userMessage = new Message(Message.Role.USER,
                Arrays.<Content>asList(new RichText(opts.question)));
        userMessage.setId(UUID.randomUUID().toString());
        conversation.addChild(conversation.getCurrentLeafId(), userMessage);
        save(historyFile, conversation);

        ModelConfig config = new ModelConfig();
        config.setProvider(opts.provider);
        config.setModelName(opts.modelName);
        config.setBaseURL(opts.modelUrl);
        config.setApiKey(opts.apiKey != null
                ? opts.apiKey
                : System.getenv("DEEPSEEK_API_KEY"));
        if (opts.maxTokens > 0) {
            config.setMaxTokens(opts.maxTokens);
        }
        if (config.getApiKey() == null || config.getApiKey().isEmpty()) {
            System.err.println("警告: 未设置 API key（--api-key 或环境变量 DEEPSEEK_API_KEY），"
                    + "请求可能返回 401");
        }

        ToolsManager tools = new ToolsManager(new CurrentTimeTool());
        if (opts.tools) {
            tools.register(new FetchUrl());
        }
        ChatRequest request = ChatEngine.requestBuilder(conversation, config, tools).build();
        request = expandFiles(request);
        request = trimHistory(request, opts.maxHistory);

        try (ChatModel model = AdapterManager.createChatModel(config)) {
            ChatEngine engine = new ChatEngine(model, tools);
            engine.stream(request, streamingListener(conversation, config), toolRoundObserver());
        }
        save(historyFile, conversation);
    }

    private ChatStreamListener streamingListener(Conversation conversation, ModelConfig config) {
        final StringBuilder fullText = new StringBuilder();
        final AtomicBoolean inReasoning = new AtomicBoolean(false);
        return new ChatStreamListener() {
            @Override
            public void onChunk(ChatChunk chunk) {
                if (chunk.getReasoning() != null) {
                    if (!inReasoning.getAndSet(true)) {
                        System.out.println();
                        print(GRAY, "[思考] ");
                    }
                    print(GRAY, chunk.getReasoning());
                    return;
                }
                if (chunk.getContent() != null) {
                    if (inReasoning.getAndSet(false)) {
                        print(RESET, "");
                        System.out.println();
                    }
                    System.out.print(chunk.getContent());
                    System.out.flush();
                    fullText.append(chunk.getContent());
                }
            }

            @Override
            public void onDone() {
                if (inReasoning.getAndSet(false)) {
                    print(RESET, "");
                }
                System.out.println();
                appendModelMessage(conversation, fullText.toString(), config);
            }

            @Override
            public void onError(Throwable t) {
                if (inReasoning.getAndSet(false)) {
                    print(RESET, "");
                }
                System.err.println();
                System.err.println("[错误] " + t);
            }
        };
    }

    private ChatEngine.ToolObserver toolRoundObserver() {
        return (round, results) -> {
            if (round.getReasoning() != null) {
                print(GRAY, "[思考] " + round.getReasoning());
                System.out.println();
            }
            for (ToolCall toolCall : round.getToolCalls()) {
                print(CYAN, "\n[工具] " + toolCall.getName() + " " + toolCall.getArgumentsJson());
                System.out.println();
                String result = results.get(toolCall.getId());
                print(YELLOW, "[结果] " + summarizeToolResult(result));
                System.out.println();
            }
        };
    }

    /**
     * 工具结果展示摘要：完整内容仍会回传给模型，这里只输出判断。
     * FetchUrl 等返回 {"ok":true/false,...} 的 JSON 会折叠为成功/失败；
     * 其他内容过长时截断。
     */
    static String summarizeToolResult(String result) {
        if (result == null || result.isEmpty()) {
            return "(无结果)";
        }
        String trimmed = result.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                JsonNode node = JSON.readTree(trimmed);
                if (node.isObject() && node.has("ok")) {
                    if (node.path("ok").asBoolean(false)) {
                        if (node.has("html")) {
                            int length = node.path("html").asText("").length();
                            return "成功（HTML，约 " + length + " 字符）";
                        }
                        // 短 JSON（如 GetCurrentTime 的结果）直接展示，方便用户看到内容
                        return trimmed.length() <= 200 ? trimmed : "成功（返回 JSON）";
                    }
                    return "失败: " + node.path("error").asText("未知错误");
                }
            } catch (Exception ignored) {
                // 不是合法 JSON，走截断逻辑
            }
        }
        if (trimmed.length() > 200) {
            return trimmed.substring(0, 200) + "…（共 " + trimmed.length() + " 字符）";
        }
        return trimmed;
    }

    private static void appendModelMessage(Conversation conversation, String text,
                                           ModelConfig config) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Message modelMessage = new Message(Message.Role.MODEL,
                Arrays.<Content>asList(new RichText(text)));
        modelMessage.setId(UUID.randomUUID().toString());
        modelMessage.setModelId(config.getModelName());
        conversation.addChild(conversation.getCurrentLeafId(), modelMessage);
    }

    /** 把 [文件: path] 标记消息展开为文件内容。 */
    private static ChatRequest expandFiles(ChatRequest request) throws IOException {
        List<ChatMessage> expanded = null;
        List<ChatMessage> messages = request.getMessages();
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            String content = message.getContent();
            if (content == null || !content.startsWith(FILE_PREFIX)
                    || !content.endsWith("]")) {
                continue;
            }
            if (expanded == null) {
                expanded = new ArrayList<>(messages);
            }
            String path = content.substring(FILE_PREFIX.length(), content.length() - 1);
            File file = new File(path);
            String text;
            if (file.isFile()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                text = "文件内容: " + path + "\n\n"
                        + new String(bytes, StandardCharsets.UTF_8);
            } else {
                text = "错误: 文件未找到 - " + path;
            }
            expanded.set(i, ChatMessage.builder()
                    .role(message.getRole())
                    .content(text)
                    .build());
        }
        return expanded == null ? request : copyRequest(request, expanded);
    }

    /** 限制发送窗口：保留 system + 最近 maxHistory 轮（每轮 user+assistant 两条）。 */
    private static ChatRequest trimHistory(ChatRequest request, int maxHistory) {
        if (maxHistory <= 0) {
            return request;
        }
        List<ChatMessage> messages = request.getMessages();
        int limit = 1 + 2 * maxHistory;
        if (messages.size() <= limit) {
            return request;
        }
        List<ChatMessage> trimmed = new ArrayList<>();
        trimmed.add(messages.get(0));
        trimmed.addAll(messages.subList(messages.size() - 2 * maxHistory, messages.size()));
        return copyRequest(request, trimmed);
    }

    private static ChatRequest copyRequest(ChatRequest base, List<ChatMessage> messages) {
        ChatRequest.Builder builder = ChatRequest.builder()
                .model(base.getModel())
                .messages(messages)
                .temperature(base.getTemperature())
                .maxTokens(base.getMaxTokens())
                .topP(base.getTopP())
                .stop(base.getStop())
                .seed(base.getSeed())
                .toolChoice(base.getToolChoice())
                .toolChoiceFunction(base.getToolChoiceFunction())
                .responseFormat(base.getResponseFormat())
                .responseFormatSchema(base.getResponseFormatSchema())
                .responseFormatName(base.getResponseFormatName())
                .apiKey(base.getApiKey());
        if (base.getTools() != null && !base.getTools().isEmpty()) {
            builder.tools(base.getTools());
        }
        return builder.build();
    }

    private static void setPrompt(Conversation conversation, String prompt) {
        String rootId = conversation.getRootMessageId();
        if (rootId != null) {
            Message root = conversation.getMessage(rootId);
            if (root != null) {
                for (Content content : root.getContents()) {
                    if (content instanceof RichText) {
                        content.overwrite(prompt);
                        return;
                    }
                }
                root.getContents().add(new RichText(prompt));
                return;
            }
        }
        Message root = new Message(Message.Role.SYSTEM,
                Arrays.<Content>asList(new RichText(prompt)));
        root.setId("root");
        conversation.addChild(null, root);
    }

    private static void addFileMessage(Conversation conversation, String filePath) {
        File file = new File(filePath);
        String absolute = file.getAbsolutePath();
        if (!file.isFile()) {
            System.err.println("警告: 文件不存在 - " + absolute);
        }
        Message message = new Message(Message.Role.USER,
                Arrays.<Content>asList(new TextFile(FILE_PREFIX + absolute + "]")));
        message.setId(UUID.randomUUID().toString());
        conversation.addChild(conversation.getCurrentLeafId(), message);
    }

    private static boolean clearFileMessages(Conversation conversation) {
        List<String> ids = new ArrayList<>();
        for (Message message : conversation.getMessages().values()) {
            for (Content content : message.getContents()) {
                if (content instanceof TextFile
                        && ((TextFile) content).getStringContent().startsWith(FILE_PREFIX)) {
                    ids.add(message.getId());
                    break;
                }
            }
        }
        for (String id : ids) {
            conversation.deleteMessage(id);
        }
        System.out.println("已移除 " + ids.size() + " 条文件消息");
        return !ids.isEmpty();
    }

    private static void printHistory(Conversation conversation) {
        System.out.println("会话 " + conversation.getId() + " 的历史记录:");
        for (Message message : conversation.getActivePath()) {
            StringBuilder text = new StringBuilder();
            for (Content content : message.getContents()) {
                if (content instanceof ToolCallContent || content instanceof Reasoning) {
                    continue;
                }
                if (content instanceof PictureFile) {
                    text.append("[图片]");
                    continue;
                }
                String s = content.getStringContent();
                if (s != null && !s.isEmpty()) {
                    if (text.length() > 0) {
                        text.append('\n');
                    }
                    text.append(s);
                }
            }
            System.out.println("来自: " + message.getRole().name().toLowerCase());
            System.out.println();
            System.out.println("说: " + text);
            System.out.println();
        }
    }

    private static Conversation newConversation(String session, String prompt) {
        Conversation conversation = new Conversation(session, session);
        Message root = new Message(Message.Role.SYSTEM,
                Arrays.<Content>asList(new RichText(prompt)));
        root.setId("root");
        conversation.addChild(null, root);
        return conversation;
    }

    private static Conversation load(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return ConversationCodec.fromJson(new String(bytes, StandardCharsets.UTF_8));
    }

    private static void save(File file, Conversation conversation) throws IOException {
        Files.write(file.toPath(),
                ConversationCodec.toJson(conversation).getBytes(StandardCharsets.UTF_8));
    }

    private static String resolveSession(File dataDir, String sessionArg) throws IOException {
        File lastFile = new File(dataDir, "last_session.txt");
        if (sessionArg != null && !sessionArg.isEmpty()) {
            Files.write(lastFile.toPath(),
                    sessionArg.getBytes(StandardCharsets.UTF_8));
            return sessionArg;
        }
        if (lastFile.isFile()) {
            String last = new String(Files.readAllBytes(lastFile.toPath()),
                    StandardCharsets.UTF_8).trim();
            if (!last.isEmpty()) {
                return last;
            }
        }
        Files.write(lastFile.toPath(), "default".getBytes(StandardCharsets.UTF_8));
        return "default";
    }

    private static String effectivePrompt(String prompt) {
        return prompt == null ? DEFAULT_PROMPT : prompt;
    }

    private void print(String colorCode, String text) {
        if (color) {
            System.out.print(colorCode + text + RESET);
        } else {
            System.out.print(text);
        }
        System.out.flush();
    }

    private static final class Options {
        boolean help;
        boolean noColor;
        String session;
        String question;
        boolean showHistory;
        boolean clearHistory;
        String prompt;
        int maxHistory = 100;
        String modelUrl = DEFAULT_URL;
        String modelName = DEFAULT_MODEL;
        String provider = "openai-chat";
        String apiKey;
        String addFile;
        boolean clearFile;
        boolean tools;
        long maxTokens;
        File dataDir = new File(System.getProperty("user.home"), ".letmeaskyou");

        boolean parse(String[] args) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "-h":
                    case "--help":
                        help = true;
                        break;
                    case "--no-color":
                        noColor = true;
                        break;
                    case "-s":
                    case "--session":
                        session = value(args, ++i, arg);
                        break;
                    case "-a":
                    case "--ask":
                        question = value(args, ++i, arg);
                        break;
                    case "-sh":
                    case "--show-history":
                        showHistory = true;
                        break;
                    case "-ch":
                    case "--clear-history":
                        clearHistory = true;
                        break;
                    case "-sp":
                    case "--set-prompt":
                        prompt = value(args, ++i, arg);
                        break;
                    case "-mh":
                    case "--set-max-history-number":
                        maxHistory = intValue(value(args, ++i, arg));
                        break;
                    case "--model-url":
                        modelUrl = value(args, ++i, arg);
                        break;
                    case "--model-name":
                        modelName = value(args, ++i, arg);
                        break;
                    case "--provider":
                        provider = value(args, ++i, arg);
                        break;
                    case "--api-key":
                        apiKey = value(args, ++i, arg);
                        break;
                    case "--max-tokens":
                        maxTokens = longValue(value(args, ++i, arg));
                        break;
                    case "-af":
                    case "--add-file":
                        addFile = value(args, ++i, arg);
                        break;
                    case "-cf":
                    case "--clear-file":
                        clearFile = true;
                        break;
                    case "--tools":
                        tools = true;
                        break;
                    case "--data-dir":
                        dataDir = new File(value(args, ++i, arg));
                        break;
                    default:
                        if (arg.startsWith("-")) {
                            System.err.println("未知选项: " + arg);
                            return false;
                        }
                        question = question == null ? arg : question + " " + arg;
                }
            }
            return true;
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length) {
                System.err.println("缺少 " + option + " 的值");
                throw new IllegalArgumentException("missing value for " + option);
            }
            return args[index];
        }

        private static int intValue(String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("数字格式错误: " + s);
            }
        }

        private static long longValue(String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("数字格式错误: " + s);
            }
        }
    }

    private static void usage() {
        System.out.println(
                "用法: ask-cli [选项]\n"
                + "选项:\n"
                + "  -s, --session <session>               设置会话 (默认: default，自动记住上次)\n"
                + "  -a, --ask <question>                  提问（也支持直接拼在命令后面）\n"
                + "  -sh, --show-history                   显示当前会话的历史记录\n"
                + "  -ch, --clear-history                  清除当前会话的历史记录\n"
                + "  -sp, --set-prompt <prompt>            设置自定义提示词\n"
                + "  -mh, --set-max-history-number <n>     请求时最多保留 n 轮历史 (默认: 100)\n"
                + "      --model-url <url>                 模型 URL (默认: https://api.deepseek.com)\n"
                + "      --model-name <name>               模型名称 (默认: deepseek-v4-flash)\n"
                + "      --provider <p>                    提供商 (默认: openai-chat)\n"
                + "      --api-key <key>                   默认读环境变量 DEEPSEEK_API_KEY\n"
                + "      --max-tokens <n>                  最大输出 token 数\n"
                + "  -af, --add-file <filepath>            添加文件到会话\n"
                + "  -cf, --clear-file                     移除文件消息\n"
                + "      --tools                           额外启用 FetchUrl 网页抓取；"
                + "GetCurrentTime 时间工具始终可用\n"
                + "      --data-dir <dir>                  会话数据目录 (默认: ~/.letmeaskyou)\n"
                + "      --no-color                        关闭彩色输出\n"
                + "  -h, --help                            显示此帮助信息\n"
                + "示例:\n"
                + "  ask-cli --session my --ask '你好'\n"
                + "  ask-cli -s my -sh\n"
                + "  ask-cli --provider anthropic --model-url https://api.deepseek.com/anthropic "
                + "--model-name deepseek-v4-flash '你好'\n"
                + "  ask-cli --tools --ask '打开 https://example.com 看看有什么'\n");
    }
}
