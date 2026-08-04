package org.bluepowerrobotics.letmeaskyou.core.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 以“消息树”组织的对话。 */
public class Conversation {
    private String id;
    private String title;
    private Map<String, Message> messages = new LinkedHashMap<>();
    private String rootMessageId;
    private String currentLeafId;
    private long createdAt;
    private long updatedAt;
    private String modelId;
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public Conversation() {
    }

    public Conversation(String id, String title) {
        this.id = id;
        this.title = title;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 注册消息但不建立父子关系（一般不用，树结构用 addChild）。 */
    public void addMessage(Message message) {
        Objects.requireNonNull(message, "message");
        if (message.getId() == null) {
            throw new IllegalArgumentException("message.id must be set before adding");
        }
        message.setConversationId(id);
        messages.put(message.getId(), message);
    }

    /**
     * 在 parentId 下挂一个子节点。parentId 为 null 时作为根消息
     * （已有根时抛异常，避免出现多根）。
     */
    public Message addChild(String parentId, Message child) {
        Objects.requireNonNull(child, "child");
        if (child.getId() == null) {
            throw new IllegalArgumentException("child.id must be set");
        }
        if (parentId == null) {
            if (rootMessageId != null) {
                throw new IllegalStateException("conversation already has a root message");
            }
            rootMessageId = child.getId();
        } else {
            Message parent = messages.get(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("parent message not found: " + parentId);
            }
            // 兄弟分支全部取消激活，新节点成为活跃分支
            for (String siblingId : parent.getChildrenIds()) {
                Message sibling = messages.get(siblingId);
                if (sibling != null) {
                    sibling.setActive(false);
                }
            }
            parent.addChildId(child.getId());
        }
        child.setParentId(parentId);
        child.setConversationId(id);
        child.setTimestamp(System.currentTimeMillis());
        child.setActive(true);
        messages.put(child.getId(), child);
        currentLeafId = child.getId();
        touch();
        return child;
    }

    /** 删除节点；其子节点整体上移一层，归到父节点下。 */
    public void deleteMessage(String messageId) {
        Message target = messages.get(messageId);
        if (target == null) {
            return;
        }
        String parentId = target.getParentId();
        List<String> orphans = new ArrayList<>(target.getChildrenIds());

        if (parentId == null) {
            // 删除根：第一个子节点顶替为根
            if (!orphans.isEmpty()) {
                rootMessageId = orphans.get(0);
                Message newRoot = messages.get(rootMessageId);
                if (newRoot != null) {
                    newRoot.setParentId(null);
                    newRoot.addChildId(null); // no-op guard
                }
            } else {
                rootMessageId = null;
            }
        } else {
            Message parent = messages.get(parentId);
            if (parent != null) {
                parent.removeChildId(messageId);
                for (String orphanId : orphans) {
                    parent.addChildId(orphanId);
                    Message orphan = messages.get(orphanId);
                    if (orphan != null) {
                        orphan.setParentId(parentId);
                    }
                }
            }
        }

        if (currentLeafId != null && currentLeafId.equals(messageId)) {
            currentLeafId = orphans.isEmpty() ? parentId : orphans.get(orphans.size() - 1);
        }
        messages.remove(messageId);
        touch();
    }

    /** 切换活跃分支：目标节点激活，其兄弟取消激活。 */
    public void switchBranch(String messageId) {
        Message target = messages.get(messageId);
        if (target == null) {
            throw new IllegalArgumentException("message not found: " + messageId);
        }
        String parentId = target.getParentId();
        if (parentId != null) {
            Message parent = messages.get(parentId);
            if (parent != null) {
                for (String siblingId : parent.getChildrenIds()) {
                    Message sibling = messages.get(siblingId);
                    if (sibling != null) {
                        sibling.setActive(siblingId.equals(messageId));
                    }
                }
            }
        }
        target.setActive(true);
        currentLeafId = messageId;
        touch();
    }

    /** 从根到指定节点的路径（不含被删节点）。 */
    public List<Message> getPathFromRoot(String messageId) {
        if (messageId == null) {
            return Collections.emptyList();
        }
        List<Message> path = new ArrayList<>();
        String cursor = messageId;
        while (cursor != null) {
            Message m = messages.get(cursor);
            if (m == null) {
                break;
            }
            path.add(0, m);
            cursor = m.getParentId();
        }
        return path;
    }

    /** 与指定消息同父的兄弟（不含自己）。 */
    public List<Message> getSiblings(String messageId) {
        Message target = messages.get(messageId);
        if (target == null || target.getParentId() == null) {
            return Collections.emptyList();
        }
        Message parent = messages.get(target.getParentId());
        if (parent == null) {
            return Collections.emptyList();
        }
        List<Message> siblings = new ArrayList<>();
        for (String siblingId : parent.getChildrenIds()) {
            if (!siblingId.equals(messageId)) {
                Message sibling = messages.get(siblingId);
                if (sibling != null) {
                    siblings.add(sibling);
                }
            }
        }
        return siblings;
    }

    /** 当前活跃分支的完整消息路径（根 → 当前叶子）。 */
    public List<Message> getActivePath() {
        return getPathFromRoot(currentLeafId);
    }

    public Message getMessage(String messageId) {
        return messages.get(messageId);
    }

    public Map<String, Message> getMessages() {
        return messages;
    }

    public void setMessages(Map<String, Message> messages) {
        this.messages = messages == null ? new LinkedHashMap<>() : messages;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRootMessageId() {
        return rootMessageId;
    }

    public void setRootMessageId(String rootMessageId) {
        this.rootMessageId = rootMessageId;
    }

    public String getCurrentLeafId() {
        return currentLeafId;
    }

    public void setCurrentLeafId(String currentLeafId) {
        this.currentLeafId = currentLeafId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<>() : metadata;
    }

    private void touch() {
        updatedAt = System.currentTimeMillis();
    }
}
