package org.bluepowerrobotics.lmau.core.conversation.contents;

/** 模型发起的工具调用（挂在一个 MODEL 消息下）。 */
public class ToolCallContent implements Content {
    private final String id;
    private final String name;
    private final String argumentsJson;
    private boolean finished = true;

    public ToolCallContent(String id, String name, String argumentsJson) {
        this.id = id;
        this.name = name;
        this.argumentsJson = argumentsJson == null ? "{}" : argumentsJson;
    }

    public String getToolCallId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    @Override
    public String getKind() {
        return "ToolCall";
    }

    @Override
    public String getStringContent() {
        return argumentsJson;
    }

    @Override
    public Object get() {
        return getStringContent();
    }

    @Override
    public boolean overwrite(Object target) {
        return false;
    }

    @Override
    public boolean append(Object difference) {
        return false;
    }

    @Override
    public void finish() {
        finished = true;
    }

    @Override
    public boolean ifFinished() {
        return finished;
    }
}
