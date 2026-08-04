package org.bluepowerrobotics.letmeaskyou.core.conversation;

import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Content;

import java.util.ArrayList;
import java.util.List;

/** 对话树中的一个节点。 */
public class Message {
    public enum Role { SYSTEM, USER, MODEL }

    private String id;
    private String parentId;            // 父节点ID（null = 根）
    private String conversationId;      // 所属对话ID
    private List<String> childrenIds = new ArrayList<>();
    private Role role;                  // SYSTEM / USER / MODEL
    private List<Content> contents = new ArrayList<>();
    private String modelId;             // 生成此消息的模型（仅MODEL角色）
    private long timestamp;
    private boolean active = true;      // 当前活跃分支标记
    private boolean finished;           // 是否完全生成

    public Message() {
    }

    public Message(Role role, List<Content> contents) {
        this.role = role;
        if (contents != null) {
            this.contents = contents;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public void setChildrenIds(List<String> childrenIds) {
        this.childrenIds = childrenIds == null ? new ArrayList<>() : childrenIds;
    }

    public void addChildId(String childId) {
        if (childId != null && !childrenIds.contains(childId)) {
            childrenIds.add(childId);
        }
    }

    public void removeChildId(String childId) {
        childrenIds.remove(childId);
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents == null ? new ArrayList<>() : contents;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
