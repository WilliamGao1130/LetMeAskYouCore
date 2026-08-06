package org.bluepowerrobotics.lmau.core.chat;

import org.bluepowerrobotics.lmau.converter.core.ChatChunk;
import org.bluepowerrobotics.lmau.converter.core.ChatMessage;
import org.bluepowerrobotics.lmau.converter.core.ChatModel;
import org.bluepowerrobotics.lmau.converter.core.ChatRequest;
import org.bluepowerrobotics.lmau.converter.core.ChatResponse;
import org.bluepowerrobotics.lmau.converter.core.ChatRole;
import org.bluepowerrobotics.lmau.converter.core.ChatStreamListener;
import org.bluepowerrobotics.lmau.converter.core.ContentPart;
import org.bluepowerrobotics.lmau.converter.core.ToolCall;
import org.bluepowerrobotics.lmau.core.config.ModelConfig;
import org.bluepowerrobotics.lmau.core.conversation.Conversation;
import org.bluepowerrobotics.lmau.core.conversation.Message;
import org.bluepowerrobotics.lmau.core.conversation.contents.PictureFile;
import org.bluepowerrobotics.lmau.core.conversation.contents.Reasoning;
import org.bluepowerrobotics.lmau.core.conversation.contents.ToolCallContent;
import org.bluepowerrobotics.lmau.core.conversation.contents.ToolResult;
import org.bluepowerrobotics.lmau.core.toolcall.ToolsManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 对话引擎：把核心的对话树/配置翻译成 AIAPIConverter 的统一请求，
 * 并驱动“模型 → 工具执行 → 模型”的循环。
 * <p>
 * 纯函数式设计：不直接修改对话树，树的变化由调用方（界面层）完成。
 * </p>
 */
public final class ChatEngine {

    /** 工具循环的最大轮数，防止模型无限调用工具。 */
    public static final int MAX_TOOL_ROUNDS = 8;

    /** 工具循环每轮的观察回调（CLI 等展示层使用）。 */
    public interface ToolObserver {
        void onToolRound(ChatResponse roundResponse, Map<String, String> results);
    }

    private final ChatModel model;
    private final ToolsManager tools;

    public ChatEngine(ChatModel model, ToolsManager tools) {
        this.model = Objects.requireNonNull(model, "model");
        this.tools = tools;
    }

    public ChatModel getModel() {
        return model;
    }

    /**
     * 非流式调用；若请求带工具且模型发起工具调用，自动执行并把结果
     * 追加进消息后继续调用，直到模型给出最终回答或超过最大轮数。
     */
    public ChatResponse complete(ChatRequest request) {
        return complete(request, null);
    }

    /**
     * 非流式调用；若请求带工具且模型发起工具调用，自动执行并把结果
     * 追加进消息后继续调用，直到模型给出最终回答或超过最大轮数。
     *
     * @param observer 可选的每轮观察回调，用于展示工具调用与结果
     */
    public ChatResponse complete(ChatRequest request, ToolObserver observer) {
        Objects.requireNonNull(request, "request");
        List<ChatMessage> messages = new ArrayList<>(request.getMessages());
        ChatRequest current = request;
        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            ChatResponse response = model.complete(current);
            if (response == null || response.getToolCalls().isEmpty()
                    || tools == null || tools.isEmpty()) {
                return response;
            }

            messages.add(ChatMessage.builder()
                    .role(ChatRole.ASSISTANT)
                    .content(response.getContent())
                    .toolCalls(response.getToolCalls())
                    .build());

            Map<String, String> results = tools.executeAll(response.getToolCalls());
            if (observer != null) {
                observer.onToolRound(response, results);
            }
            for (ToolCall toolCall : response.getToolCalls()) {
                String result = results.get(toolCall.getId());
                messages.add(ChatMessage.tool(toolCall.getId(),
                        result == null ? "{\"ok\":false,\"error\":\"no result\"}" : result));
            }
            current = rebuild(current, messages);
        }
        throw new IllegalStateException(
                "Tool call loop exceeded " + MAX_TOOL_ROUNDS + " rounds");
    }

    /**
     * 流式调用。带工具时会在流式响应中识别工具调用（适配器把增量片段拼成
     * tool_calls 挂在收尾块上），执行工具后继续下一轮流式，全程单次生成。
     */
    public void stream(ChatRequest request, ChatStreamListener listener) {
        stream(request, listener, null);
    }

    /**
     * 流式调用（带工具轮观察回调）。
     *
     * @param observer 工具轮观察回调，可为 null
     */
    public void stream(ChatRequest request, ChatStreamListener listener,
                       ToolObserver observer) {
        boolean hasTools = tools != null && !tools.isEmpty()
                && request.getTools() != null && !request.getTools().isEmpty();
        if (!hasTools) {
            model.stream(request, listener);
            return;
        }
        streamRound(request, listener, observer, 0);
    }

    /**
     * 单轮流式。若本轮响应末尾携带工具调用，执行并把结果回喂后递归下一轮；
     * 否则结束。
     */
    private void streamRound(final ChatRequest request, final ChatStreamListener listener,
                             final ToolObserver observer, final int round) {
        final java.util.concurrent.atomic.AtomicReference<List<ToolCall>> roundToolCalls =
                new java.util.concurrent.atomic.AtomicReference<List<ToolCall>>();
        final StringBuilder roundText = new StringBuilder();

        ChatStreamListener wrapped = new ChatStreamListener() {
            @Override
            public void onChunk(ChatChunk chunk) {
                if (chunk.getToolCalls() != null && !chunk.getToolCalls().isEmpty()) {
                    roundToolCalls.set(chunk.getToolCalls());
                }
                if (chunk.getContent() != null) {
                    roundText.append(chunk.getContent());
                }
                if (listener != null) {
                    listener.onChunk(chunk);
                }
            }

            @Override
            public void onDone() {
                List<ToolCall> calls = roundToolCalls.get();
                if (calls == null || calls.isEmpty() || round >= MAX_TOOL_ROUNDS - 1) {
                    if (listener != null) {
                        listener.onDone();
                    }
                    return;
                }

                List<ChatMessage> messages = new ArrayList<>(request.getMessages());
                String text = roundText.length() == 0 ? null : roundText.toString();
                messages.add(ChatMessage.builder()
                        .role(ChatRole.ASSISTANT)
                        .content(text)
                        .toolCalls(calls)
                        .build());

                Map<String, String> results = tools.executeAll(calls);
                if (observer != null) {
                    observer.onToolRound(ChatResponse.builder()
                            .content(text)
                            .toolCalls(calls)
                            .build(), results);
                }
                for (ToolCall toolCall : calls) {
                    String result = results.get(toolCall.getId());
                    messages.add(ChatMessage.tool(toolCall.getId(),
                            result == null ? "{\"ok\":false,\"error\":\"no result\"}" : result));
                }
                streamRound(rebuild(request, messages), listener, observer, round + 1);
            }

            @Override
            public void onError(Throwable t) {
                if (listener != null) {
                    listener.onError(t);
                }
            }
        };
        model.stream(request, wrapped);
    }

    /**
     * 构建请求：把对话当前活跃分支（根 → 当前叶子）转成统一消息列表。
     * modelName 必填；tools 可为 null。
     */
    public static ChatRequest.Builder requestBuilder(
            Conversation conversation, ModelConfig modelConfig, ToolsManager tools) {
        Objects.requireNonNull(conversation, "conversation");
        Objects.requireNonNull(modelConfig, "modelConfig");
        if (modelConfig.getModelName() == null || modelConfig.getModelName().isEmpty()) {
            throw new IllegalArgumentException("modelName must be set");
        }

        List<ChatMessage> messages = new ArrayList<>();
        for (Message message : conversation.getActivePath()) {
            messages.addAll(toChatMessages(message));
        }

        ChatRequest.Builder builder = ChatRequest.builder()
                .model(modelConfig.getModelName())
                .messages(messages)
                .apiKey(modelConfig.getApiKey());
        if (modelConfig.getMaxTokens() > 0) {
            builder.maxTokens((int) modelConfig.getMaxTokens());
        }
        if (tools != null && !tools.isEmpty()) {
            builder.tools(tools.toConverterTools());
        }
        return builder;
    }

    /** 把核心的 Message 转成一个或多个统一 ChatMessage。 */
    public static List<ChatMessage> toChatMessages(Message message) {
        if (message == null) {
            return Collections.emptyList();
        }

        List<ToolCallContent> toolCalls = new ArrayList<>();
        List<ToolResult> toolResults = new ArrayList<>();
        List<ContentPart> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for (org.bluepowerrobotics.lmau.core.conversation.contents.Content content
                : message.getContents()) {
            if (content instanceof ToolCallContent) {
                toolCalls.add((ToolCallContent) content);
            } else if (content instanceof ToolResult) {
                toolResults.add((ToolResult) content);
            } else if (content instanceof PictureFile) {
                PictureFile picture = (PictureFile) content;
                parts.add(ContentPart.imageUrl("data:" + picture.getMimeType()
                        + ";base64," + picture.getStringContent()));
            } else if (content instanceof Reasoning) {
                // 推理内容不回传给模型
            } else {
                String s = content.getStringContent();
                if (s != null && !s.isEmpty()) {
                    text.append(s);
                }
            }
        }

        List<ChatMessage> out = new ArrayList<>();
        for (ToolResult toolResult : toolResults) {
            out.add(ChatMessage.tool(toolResult.getToolCallId(),
                    toolResult.getStringContent()));
        }
        if (!toolResults.isEmpty()) {
            return out;
        }

        ChatRole role;
        switch (message.getRole()) {
            case SYSTEM:
                role = ChatRole.SYSTEM;
                break;
            case MODEL:
                role = ChatRole.ASSISTANT;
                break;
            default:
                role = ChatRole.USER;
        }

        ChatMessage.Builder builder = ChatMessage.builder().role(role);
        if (!parts.isEmpty()) {
            if (text.length() > 0) {
                parts.add(0, ContentPart.text(text.toString()));
            }
            builder.contentParts(parts);
        } else {
            builder.content(text.toString());
        }
        if (!toolCalls.isEmpty()) {
            List<ToolCall> converterCalls = new ArrayList<>();
            for (ToolCallContent toolCall : toolCalls) {
                converterCalls.add(new ToolCall(
                        toolCall.getToolCallId(),
                        toolCall.getName(),
                        toolCall.getArgumentsJson()));
            }
            builder.toolCalls(converterCalls);
        }
        out.add(builder.build());
        return out;
    }

    private static ChatRequest rebuild(ChatRequest base, List<ChatMessage> messages) {
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
                .reasoningEffort(base.getReasoningEffort())
                .apiKey(base.getApiKey());
        if (base.getTools() != null && !base.getTools().isEmpty()) {
            builder.tools(base.getTools());
        }
        return builder.build();
    }
}
