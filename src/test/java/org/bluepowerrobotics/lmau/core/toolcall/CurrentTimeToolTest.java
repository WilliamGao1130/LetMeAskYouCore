package org.bluepowerrobotics.lmau.core.toolcall;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentTimeToolTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void returnsBeijingTimeToTheSecond() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("timezone", "Asia/Shanghai");

        JsonNode node = JSON.readTree(new CurrentTimeTool().execute(args));

        assertTrue(node.path("ok").asBoolean());
        assertEquals("Asia/Shanghai", node.path("timezone").asText());
        assertEquals(480, node.path("utcOffsetMinutes").asInt());
        assertTrue(node.path("iso").asText().endsWith("+08:00"));
        long now = System.currentTimeMillis() / 1000;
        assertTrue(Math.abs(node.path("epochSeconds").asLong() - now) <= 5);
        assertEquals(8, node.path("time").asText().length());
    }

    @Test
    void invalidTimezoneReturnsError() throws Exception {
        Map<String, Object> args = new HashMap<>();
        args.put("timezone", "Mars/Olympus");

        JsonNode node = JSON.readTree(new CurrentTimeTool().execute(args));

        assertTrue(!node.path("ok").asBoolean());
        assertTrue(node.path("error").asText().contains("失败"));
    }

    @Test
    void defaultUsesSystemTimezone() throws Exception {
        JsonNode node = JSON.readTree(new CurrentTimeTool().execute(new HashMap<String, Object>()));

        assertTrue(node.path("ok").asBoolean());
        assertTrue(node.path("epochSeconds").asLong() > 0);
    }
}
