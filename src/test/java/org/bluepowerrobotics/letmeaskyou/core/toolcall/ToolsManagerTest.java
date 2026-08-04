package org.bluepowerrobotics.letmeaskyou.core.toolcall;

import org.bluepowerrobotics.converter.core.ToolCall;
import org.bluepowerrobotics.converter.core.ToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolsManagerTest {

    private static class EchoTool implements Tool {
        @Override
        public String getName() {
            return "Echo";
        }

        @Override
        public String getDescription() {
            return "回显输入";
        }

        @Override
        public String getParametersJson() {
            return "{\"type\":\"object\",\"properties\":"
                    + "{\"text\":{\"type\":\"string\"}},\"required\":[\"text\"]}";
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            return "echo:" + arguments.get("text");
        }
    }

    @Test
    void toConverterToolsUsesSchema() {
        ToolsManager manager = new ToolsManager(new EchoTool());

        List<ToolDefinition> definitions = manager.toConverterTools();

        assertEquals(1, definitions.size());
        assertEquals("Echo", definitions.get(0).getName());
        assertTrue(definitions.get(0).getParametersJson().contains("\"text\""));
    }

    @Test
    void executeAllRunsToolsAndReturnsById() {
        ToolsManager manager = new ToolsManager(new EchoTool());

        Map<String, String> results = manager.executeAll(Collections.singletonList(
                new ToolCall("call-1", "Echo", "{\"text\":\"hello\"}")));

        assertEquals("echo:hello", results.get("call-1"));
    }

    @Test
    void unknownToolReturnsErrorJson() {
        ToolsManager manager = new ToolsManager();

        String result = manager.execute(new ToolCall("call-1", "Nope", "{}"));

        assertTrue(result.contains("\"ok\":false"));
        assertTrue(result.contains("unknown tool"));
    }

    @Test
    void throwingToolReturnsErrorJson() {
        Tool boom = new EchoTool() {
            @Override
            public String execute(Map<String, Object> arguments) {
                throw new IllegalStateException("boom");
            }
        };
        ToolsManager manager = new ToolsManager(boom);

        String result = manager.execute(new ToolCall("call-1", "Echo", "{}"));

        assertTrue(result.contains("\"ok\":false"));
        assertTrue(result.contains("boom"));
    }
}
