package org.bluepowerrobotics.lmau.core.conversation.contents;

public class ToolResult implements Content{
    private final String toolCallId;
    private final StringBuffer result = new StringBuffer();
    private boolean finished = false;

    public ToolResult(String toolCallId, String result) {
        this.toolCallId = toolCallId;
        if (result != null) {
            this.result.append(result);
        }
    }

    public String getToolCallId() {
        return toolCallId;
    }

    @Override
    public String getKind() {
        return "ToolResult";
    }

    @Override
    public String getStringContent() {
        return result.toString();
    }

    @Override
    public Object get() {
        return result.toString();
    }

    @Override
    public boolean overwrite(Object target) {
        if (target instanceof String) {
            result.setLength(0);
            result.append(target);
            return true;
        }
        return false;
    }

    @Override
    public boolean append(Object difference) {
        if (difference instanceof String) {
            result.append(difference);
            return true;
        }
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
