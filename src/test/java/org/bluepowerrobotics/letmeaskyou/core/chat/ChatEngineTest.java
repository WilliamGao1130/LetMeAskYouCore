package org.bluepowerrobotics.letmeaskyou.core.chat;

import org.bluepowerrobotics.converter.core.ChatChunk;
import org.bluepowerrobotics.converter.core.ChatMessage;
import org.bluepowerrobotics.converter.core.ChatModel;
import org.bluepowerrobotics.converter.core.ChatRequest;
import org.bluepowerrobotics.converter.core.ChatResponse;
import org.bluepowerrobotics.converter.core.ChatRole;
import org.bluepowerrobotics.converter.core.ChatStreamListener;
import org.bluepowerrobotics.converter.core.FinishReason;
import org.bluepowerrobotics.converter.core.ToolCall;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatEngineTest {

    private static class AddTool implements org.bluepowerrobotics.letmeaskyou.core.toolcall.Tool {
        @Override
        public String getName() {
            return "Add";
        }

        @Override
        public String getDescription() {
            return "两数相加";
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            int a = ((Number) arguments.get("a")).intValue();
            int b = ((Number) arguments.get("b")).intValue();
            return String.valueOf(a + b);
        }
    }

    private static class FakeChatModel implements ChatModel {
        final Deque<ChatResponse> responses = new ArrayDeque<>();
        final Deque<List<ToolCall>> streamToolCallRounds = new ArrayDeque<>();
        final AtomicInteger completeCalls = new AtomicInteger();
        final AtomicInteger streamCalls = new AtomicInteger();
        ChatRequest lastRequest;
        ChatRequest lastStreamedRequest;

        @Override
        public ChatResponse complete(ChatRequest request) {
            completeCalls.incrementAndGet();
            lastRequest = request;
            return responses.isEmpty() ? null : responses.poll();
        }

        @Override
        public void stream(ChatRequest request, ChatStreamListener listener) {
            streamCalls.incrementAndGet();
            lastStreamedRequest = request;
            List<ToolCall> calls = streamToolCallRounds.poll();
            if (calls != null) {
                listener.onChunk(new ChatChunk(null, null, calls, FinishReason.TOOL_CALLS));
            } else {
                listener.onChunk(new ChatChunk("流式答案", null));
            }
            listener.onDone();
        }

        @Override
        public void close() {
        }
    }

    @Test
    void toolLoopFeedsResultsBackAndReturnsFinalAnswer() {
        FakeChatModel fake = new FakeChatModel();
        fake.responses.add(ChatResponse.builder()
                .content("")
                .finishReason(FinishReason.TOOL_CALLS)
                .addToolCall(new ToolCall("call-1", "Add", "{\"a\":20,\"b\":22}"))
                .build());
        fake.responses.add(ChatResponse.builder()
                .content("答案是 42")
                .finishReason(FinishReason.STOP)
                .build());

        org.bluepowerrobotics.letmeaskyou.core.toolcall.ToolsManager tools =
                new org.bluepowerrobotics.letmeaskyou.core.toolcall.ToolsManager(new AddTool());
        ChatEngine engine = new ChatEngine(fake, tools);
        ChatRequest request = ChatRequest.builder()
                .model("test")
                .addMessage(ChatMessage.user("20+22?"))
                .tools(tools.toConverterTools())
                .build();

        ChatResponse finalResponse = engine.complete(request);

        assertEquals("答案是 42", finalResponse.getContent());
        assertEquals(2, fake.completeCalls.get());

        // 第二轮请求应包含 assistant 工具调用消息和 TOOL 结果消息
        boolean hasAssistantToolCall = false;
        boolean hasToolResult = false;
        for (ChatMessage m : fake.lastRequest.getMessages()) {
            if (m.getRole() == ChatRole.ASSISTANT && !m.getToolCalls().isEmpty()) {
                hasAssistantToolCall = true;
            }
            if (m.getRole() == ChatRole.TOOL && "42".equals(m.getContent())) {
                hasToolResult = true;
            }
        }
        assertTrue(hasAssistantToolCall, "assistant 工具调用消息应回传");
        assertTrue(hasToolResult, "工具结果应作为 TOOL 消息回传");
    }

    @Test
    void streamWithoutToolsDelegatesToModel() {
        AtomicInteger chunks = new AtomicInteger();
        FakeChatModel fake = new FakeChatModel() {
            @Override
            public void stream(ChatRequest request, ChatStreamListener listener) {
                listener.onChunk(new ChatChunk("hi", null));
                listener.onDone();
            }
        };
        ChatEngine engine = new ChatEngine(fake, null);

        engine.stream(ChatRequest.builder().model("test")
                .addMessage(ChatMessage.user("hello"))
                .build(), new ChatStreamListener() {
            @Override
            public void onChunk(ChatChunk chunk) {
                if (chunk.getContent() != null) {
                    chunks.incrementAndGet();
                }
            }

            @Override
            public void onDone() {
            }

            @Override
            public void onError(Throwable error) {
            }
        });

        assertEquals(1, chunks.get());
    }

    @Test
    void streamRecognizesToolCallsAndStreamsNextRound() {
        FakeChatModel fake = new FakeChatModel();
        fake.streamToolCallRounds.add(Collections.singletonList(
                new ToolCall("call-1", "Add", "{\"a\":1,\"b\":1}")));

        org.bluepowerrobotics.letmeaskyou.core.toolcall.ToolsManager tools =
                new org.bluepowerrobotics.letmeaskyou.core.toolcall.ToolsManager(new AddTool());
        ChatEngine engine = new ChatEngine(fake, tools);
        ChatRequest request = ChatRequest.builder()
                .model("test")
                .addMessage(ChatMessage.user("1+1?"))
                .tools(tools.toConverterTools())
                .build();

        AtomicInteger rounds = new AtomicInteger();
        AtomicReference<String> streamedText = new AtomicReference<>();
        engine.stream(request, new ChatStreamListener() {
            @Override
            public void onChunk(ChatChunk chunk) {
                if (chunk.getContent() != null) {
                    streamedText.set(chunk.getContent());
                }
            }

            @Override
            public void onDone() {
            }

            @Override
            public void onError(Throwable error) {
                throw new AssertionError(error);
            }
        }, (round, results) -> {
            assertEquals("2", results.get("call-1"));
            rounds.incrementAndGet();
        });

        assertEquals(0, fake.completeCalls.get(), "全程不应使用非流式 complete");
        assertEquals(2, fake.streamCalls.get(), "工具轮 + 最终轮各一次流式");
        assertEquals(1, rounds.get());
        assertEquals("流式答案", streamedText.get());

        boolean hasToolResult = false;
        for (ChatMessage m : fake.lastStreamedRequest.getMessages()) {
            if (m.getRole() == ChatRole.TOOL && "2".equals(m.getContent())) {
                hasToolResult = true;
            }
        }
        assertTrue(hasToolResult, "下一轮流式请求应包含工具结果消息");
    }
}
