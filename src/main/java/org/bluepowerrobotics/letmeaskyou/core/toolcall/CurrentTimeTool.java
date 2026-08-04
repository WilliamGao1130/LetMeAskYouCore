package org.bluepowerrobotics.letmeaskyou.core.toolcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * 当前时间工具：让模型知道“现在几点”。纯 JDK 实现，跨平台。
 * 返回 ISO 时间（精确到秒）、日期、星期、时区与时区偏移、Unix 时间戳。
 */
public class CurrentTimeTool implements Tool {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String defaultTimezone; // null → 系统时区

    public CurrentTimeTool() {
        this(null);
    }

    public CurrentTimeTool(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    @Override
    public String getName() {
        return "GetCurrentTime";
    }

    @Override
    public String getDescription() {
        return "获取当前时间（精确到秒），返回 ISO 时间、日期、星期、时区与时区偏移、Unix 时间戳。"
                + "参数：timezone (string, 可选，IANA 时区名如 Asia/Shanghai，缺省用系统时区)。";
    }

    @Override
    public String getParametersJson() {
        return "{\"type\":\"object\",\"properties\":{"
                + "\"timezone\":{\"type\":\"string\","
                + "\"description\":\"IANA 时区名，如 Asia/Shanghai；缺省用系统时区\"}"
                + "},\"required\":[]}";
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        try {
            String timezone = null;
            Object zoneObj = arguments == null ? null : arguments.get("timezone");
            if (zoneObj != null) {
                timezone = String.valueOf(zoneObj);
            }
            if (timezone == null || timezone.trim().isEmpty()) {
                timezone = defaultTimezone;
            }
            ZonedDateTime now = timezone == null || timezone.trim().isEmpty()
                    ? ZonedDateTime.now()
                    : ZonedDateTime.now(ZoneId.of(timezone.trim()));

            ObjectNode node = JSON.createObjectNode();
            node.put("ok", true);
            node.put("iso", now.format(ISO));
            node.put("date", now.toLocalDate().toString());
            node.put("time", now.format(TIME));
            node.put("weekday", now.getDayOfWeek().name().toLowerCase(Locale.ROOT));
            node.put("timezone", now.getZone().getId());
            node.put("utcOffsetMinutes", now.getOffset().getTotalSeconds() / 60);
            node.put("epochSeconds", now.toEpochSecond());
            return node.toString();
        } catch (Exception e) {
            ObjectNode node = JSON.createObjectNode();
            node.put("ok", false);
            node.put("error", "时间获取失败: " + e.getMessage());
            return node.toString();
        }
    }
}
