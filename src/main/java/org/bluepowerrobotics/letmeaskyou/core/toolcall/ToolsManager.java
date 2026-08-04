package org.bluepowerrobotics.letmeaskyou.core.toolcall;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bluepowerrobotics.converter.core.ToolCall;
import org.bluepowerrobotics.converter.core.ToolDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * 工具注册表与执行器：把核心的 {@link Tool} 暴露给模型，
 * 并处理模型发来的 {@link ToolCall}。
 */
public class ToolsManager {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public ToolsManager() {
    }

    public ToolsManager(Tool... initialTools) {
        if (initialTools != null) {
            for (Tool tool : initialTools) {
                register(tool);
            }
        }
    }

    public void register(Tool tool) {
        if (tool == null || tool.getName() == null || tool.getName().isEmpty()) {
            throw new IllegalArgumentException("tool must have a non-empty name");
        }
        tools.put(tool.getName(), tool);
    }

    public boolean isEmpty() {
        return tools.isEmpty();
    }

    public boolean has(String name) {
        return tools.containsKey(name);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public List<Tool> getAll() {
        return new ArrayList<>(tools.values());
    }

    /** 转成 converter 的工具定义，随请求发给模型。 */
    public List<ToolDefinition> toConverterTools() {
        List<ToolDefinition> result = new ArrayList<>();
        for (Tool tool : tools.values()) {
            result.add(new ToolDefinition(
                    tool.getName(), tool.getDescription(), tool.getParametersJson()));
        }
        return result;
    }

    /** 执行一批工具调用，返回 toolCallId → 结果字符串。 */
    public Map<String, String> executeAll(List<ToolCall> calls) {
        Map<String, String> results = new LinkedHashMap<>();
        if (calls != null) {
            for (ToolCall call : calls) {
                results.put(call.getId(), execute(call));
            }
        }
        return results;
    }

    /** 执行单个工具调用；任何异常都转成 JSON 错误结果，避免中断对话。 */
    public String execute(ToolCall call) {
        Tool tool = tools.get(call.getName());
        if (tool == null) {
            return "{\"ok\":false,\"error\":\"unknown tool: " + escape(call.getName()) + "\"}";
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> args = JSON.readValue(
                    call.getArgumentsJson(), Map.class);
            Object result = tool.execute(args == null
                    ? new LinkedHashMap<String, Object>()
                    : args);
            return result == null ? "null" : String.valueOf(result);
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"tool " + escape(tool.getName())
                    + " failed: " + escape(String.valueOf(e.getMessage())) + "\"}";
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
