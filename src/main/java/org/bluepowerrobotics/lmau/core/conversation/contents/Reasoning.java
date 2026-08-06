package org.bluepowerrobotics.lmau.core.conversation.contents;

public class Reasoning implements Content {
    private final StringBuffer content = new StringBuffer();

    public Reasoning(String content){
        this.content.append(content);
    }

    @Override
    public String getKind() {
        return "Reasoning";
    }

    @Override
    public String getStringContent() {
        return content.toString();
    }

    @Override
    public Object get() {
        return content.toString();
    }

    @Override
    public boolean overwrite(Object target) {
        if(target instanceof String){
            content.setLength(0);
            content.append(target);
            return true;
        }else {
            return false;
        }
    }

    @Override
    public boolean append(Object difference) {
        if(difference instanceof String){
            content.append(difference);
            return true;
        }else {
            return false;
        }
    }

    private boolean finished=false;
    @Override
    public void finish() {
        finished = true;
    }

    @Override
    public boolean ifFinished() {
        return finished;
    }}
