package org.bluepowerrobotics;

import org.bluepowerrobotics.converter.core.ChatChunk;
import org.bluepowerrobotics.converter.core.ChatModel;
import org.bluepowerrobotics.converter.core.ChatRequest;
import org.bluepowerrobotics.converter.core.ChatStreamListener;
import org.bluepowerrobotics.letmeaskyou.core.adapter.AdapterManager;
import org.bluepowerrobotics.letmeaskyou.core.chat.ChatEngine;
import org.bluepowerrobotics.letmeaskyou.core.config.ModelConfig;
import org.bluepowerrobotics.letmeaskyou.core.conversation.Conversation;
import org.bluepowerrobotics.letmeaskyou.core.conversation.Message;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.RichText;
import org.bluepowerrobotics.letmeaskyou.core.toolcall.FetchUrl;
import org.bluepowerrobotics.letmeaskyou.core.toolcall.Tool;
import org.bluepowerrobotics.letmeaskyou.core.toolcall.ToolsManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 命令行演示入口：用一行命令验证“配置 → ChatModel → 对话引擎 → 工具”链路。
 * <pre>
 * chat --provider openai-chat --model deepseek-v4-flash \
 *      --base-url http://127.0.0.1:19725/v1 [--api-key sk-xxx] [--max-tokens 4096] "你好"
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        String command = args[0];
        if ("chat".equals(command)) {
            chat(args);
        } else if ("tools".equals(command)) {
            listTools();
        } else {
            usage();
        }
    }

    private static void chat(String[] args) throws Exception {
        ModelConfig config = new ModelConfig();
        config.setProvider("openai-chat");
        List<String> positional = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if ("--provider".equals(arg)) {
                config.setProvider(requireValue(args, ++i, "--provider"));
            } else if ("--model".equals(arg)) {
                config.setModelName(requireValue(args, ++i, "--model"));
            } else if ("--base-url".equals(arg)) {
                config.setBaseURL(requireValue(args, ++i, "--base-url"));
            } else if ("--api-key".equals(arg)) {
                config.setApiKey(requireValue(args, ++i, "--api-key"));
            } else if ("--max-tokens".equals(arg)) {
                config.setMaxTokens(Long.parseLong(requireValue(args, ++i, "--max-tokens")));
            } else {
                positional.add(arg);
            }
        }
        if (config.getModelName() == null || config.getModelName().isEmpty()) {
            System.err.println("--model 必填");
            usage();
            return;
        }
        if (positional.isEmpty()) {
            System.err.println("缺少要发送的文本");
            usage();
            return;
        }

        Conversation conversation = new Conversation(
                UUID.randomUUID().toString(), "cli-demo");
        Message root = new Message(Message.Role.SYSTEM,
                Arrays.<org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Content>asList(
                        new RichText("You are a helpful assistant.")));
        root.setId("root");
        conversation.addChild(null, root);
        Message user = new Message(Message.Role.USER,
                Arrays.<org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Content>asList(
                        new RichText(String.join(" ", positional))));
        user.setId("user");
        conversation.addChild("root", user);

        ToolsManager tools = new ToolsManager(new FetchUrl());
        ChatRequest request = ChatEngine.requestBuilder(conversation, config, tools).build();

        System.out.println("[provider=" + config.getProvider()
                + " model=" + config.getModelName() + "]");
        try (ChatModel model = AdapterManager.createChatModel(config)) {
            ChatEngine engine = new ChatEngine(model, tools);
            engine.stream(request, new ChatStreamListener() {
                @Override
                public void onChunk(ChatChunk chunk) {
                    if (chunk.getContent() != null) {
                        System.out.print(chunk.getContent());
                    }
                }

                @Override
                public void onDone() {
                    System.out.println();
                }

                @Override
                public void onError(Throwable error) {
                    System.err.println();
                    System.err.println("[error] " + error);
                }
            });
        }
    }

    private static void listTools() {
        ToolsManager tools = new ToolsManager(new FetchUrl());
        System.out.println("registered tools:");
        for (Tool tool : tools.getAll()) {
            System.out.println("  - " + tool.getName() + ": " + tool.getDescription());
        }
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException("missing value for " + option);
        }
        return args[index];
    }

    private static void usage() {
        System.out.println(
                "用法:\n"
                + "  chat --provider <p> --model <m> [--base-url <u>] [--api-key <k>] "
                + "[--max-tokens <n>] \"文本\"\n"
                + "  tools\n"
                + "provider: openai-chat | openai-responses | anthropic | dashscope | gemini | custom\n"
                + "示例:\n"
                + "  chat --provider anthropic --model deepseek-v4-flash "
                + "--base-url https://api.deepseek.com/anthropic --api-key sk-xxx \"你好\"\n");
    }
}
