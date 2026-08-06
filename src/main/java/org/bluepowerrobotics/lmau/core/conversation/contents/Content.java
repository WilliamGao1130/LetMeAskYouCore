package org.bluepowerrobotics.lmau.core.conversation.contents;

public interface Content {
    public String getKind();
    public String getStringContent();
    public Object get();
    public boolean overwrite(Object target);
    public boolean append(Object difference);
    public void finish();
    public boolean ifFinished();
}
