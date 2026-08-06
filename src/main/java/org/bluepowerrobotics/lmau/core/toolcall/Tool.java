package org.bluepowerrobotics.lmau.core.toolcall;

import java.util.Map;

/**
 * 一个可被模型调用的工具。参数通过 JSON Schema 描述，
 * 执行结果以字符串返回（一般建议返回 JSON，便于模型解读）。
 */
public interface Tool {
    String getName();

    String getDescription();

    /** 参数的 JSON Schema：{"type":"object","properties":{...},"required":[...]}。 */
    default String getParametersJson() {
        return "{\"type\":\"object\",\"properties\":{}}";
    }

    /** 执行工具，返回结果字符串。抛出的异常会被 ToolsManager 包装为错误结果。 */
    String execute(Map<String, Object> arguments) throws Exception;
}
