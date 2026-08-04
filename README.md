# LetMeAskYouCore(LMAU-Core)

跨平台 AI 聊天软件的平台无关核心：对话树、模型接入、工具调用、存储接口。
界面和持久化由宿主应用实现，本库只依赖 JDK 与
[AIAPIConverter](../AIAPIConverter/README.md)（统一的多厂商大模型适配层）。

## 构建

开发时通过 Gradle 复合构建直接引用同目录下的 AIAPIConverter 源码：

```bash
./gradlew test build
```

产物：`build/libs/LetMeAskYouCore-1.0-SNAPSHOT.jar`。

命令行演示（在 IDE 中运行 `org.bluepowerrobotics.Main`）：

```bash
chat --provider anthropic --model deepseek-v4-flash \
     --base-url https://api.deepseek.com/anthropic --api-key sk-xxx "你好"
```

## 架构

```
adapter/    AdapterManager：provider 名 → AIAPIConverter 的 ChatModel
chat/       ChatEngine：对话树 → 统一请求；模型/工具循环；流式
config/     ModelConfig / SettingsConfig / ToolsConfig
conversation/ 消息树（Message、Conversation、ConversationController）
contents/   消息内容类型（RichText、PictureFile、ToolResult、ToolCallContent …）
storage/    持久化接口 + memory/ 参考实现（真实实现如 SQLite 由宿主提供）
toolcall/   Tool 接口、ToolsManager 注册表/执行器、FetchUrl 示例工具
web/        WebFetcher（HtmlUnit，供 FetchUrl 使用）
```

## 模型配置

`ModelConfig.provider` 与 AIAPIConverter 对齐：

| provider | 说明 |
| --- | --- |
| `openai-chat` | OpenAI Chat Completions（含任意兼容端点） |
| `openai-responses` | OpenAI Responses API |
| `anthropic` | Anthropic Messages API |
| `dashscope` | 阿里云百炼 DashScope |
| `gemini` | Google Gemini generateContent |
| 其他 | 按 OpenAI Chat Completions 兼容端点处理，配 `baseURL` |

示例：DeepSeek 的 Anthropic 兼容接口

```java
ModelConfig config = new ModelConfig();
config.setProvider("anthropic");
config.setBaseURL("https://api.deepseek.com/anthropic");
config.setModelName("deepseek-v4-flash");
config.setApiKey("sk-xxx");
```

## 基本用法

```java
// 1. 建一个对话并说话
Conversation conversation = new Conversation(UUID.randomUUID().toString(), "t");
Message root = new Message(Message.Role.SYSTEM, List.of(new RichText("你是助手")));
root.setId("root");
conversation.addChild(null, root);
Message user = new Message(Message.Role.USER, List.of(new RichText("1+1=?")));
user.setId("u1");
conversation.addChild("root", user);

// 2. 注册工具（可选）
ToolsManager tools = new ToolsManager(new FetchUrl());

// 3. 调模型（非流式；工具调用会自动执行并回喂结果）
try (ChatModel model = AdapterManager.createChatModel(config)) {
    ChatEngine engine = new ChatEngine(model, tools);
    ChatResponse response = engine.complete(
            ChatEngine.requestBuilder(conversation, config, tools).build());
    System.out.println(response.getContent());
}
```

流式用 `engine.stream(request, listener)`。带工具时会在流式响应中直接识别
`tool_calls`（各适配器把增量片段拼成完整调用挂在收尾块上），执行工具后继续
下一轮流式，全程单次生成、逐字输出。

## CLI（仿 “那我问你 / ask-ai”）

基于核心实现了一个命令行测试程序 `org.bluepowerrobotics.letmeaskyou.cli.AskCLI`，
行为对齐原来的 bash+python 脚本，并增加思考链/工具结果展示：

```bash
./gradlew fatJar
java -jar build/libs/LetMeAskYouCore-1.0-SNAPSHOT-all.jar \
  --session my --ask "你好"
```

常用选项（与旧脚本一致）：

| 选项 | 说明 |
| --- | --- |
| `-s, --session` | 会话名（默认 default，自动记住上次） |
| `-a, --ask` | 提问；也支持直接拼在命令后面 |
| `-sh, --show-history` | 显示当前会话历史 |
| `-ch, --clear-history` | 清除当前会话 |
| `-sp, --set-prompt` | 设置系统提示词 |
| `-mh, --set-max-history-number` | 请求窗口轮数（默认 100） |
| `--model-url / --model-name / --provider / --api-key` | 模型配置（默认 DeepSeek openai-chat / deepseek-v4-flash，key 读 `DEEPSEEK_API_KEY`） |
| `-af, --add-file` / `-cf, --clear-file` | 添加/移除文件 |
| `--tools` | 启用 FetchUrl 工具；工具调用与结果在流式过程中实时展示 |
| `--data-dir` | 会话数据目录（默认 `~/.letmeaskyou`，JSON 持久化） |

思考链展示依赖 AIAPIConverter 的 reasoning 支持：
DeepSeek `reasoning_content`、Anthropic `thinking`、OpenAI Responses
`reasoning`、Gemini `thought`、DashScope `reasoningContent` 都会进入
`ChatChunk.reasoning` / `ChatResponse.reasoning`，CLI 以灰色输出。

## 与宿主应用的分工

- 本库负责：对话树操作、请求构造、工具定义与执行循环、provider 映射。
- 宿主负责：`storage` 接口的实现（SQLite/文件/云）、设置/模型的 UI、
  消息树的可视化与用户输入捕获。
- 多模态：`PictureFile`（base64 + mime）会自动转成 `ContentPart` 随请求发送；
  支持该能力的模型即可用。
