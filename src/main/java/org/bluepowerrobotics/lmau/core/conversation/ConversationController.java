package org.bluepowerrobotics.lmau.core.conversation;

import org.bluepowerrobotics.lmau.core.conversation.contents.Content;
import org.bluepowerrobotics.lmau.core.conversation.contents.RichText;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 对话树的操作入口：分支、重生成、编辑、删除。 */
public class ConversationController {
    private Conversation conversation;

    public ConversationController() {
    }

    public ConversationController(Conversation conversation) {
        this.conversation = conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Conversation getConversation() {
        return conversation;
    }

    /** 在某节点下添加子节点（新消息自动成为活跃分支）。 */
    public Message addChild(String parentId, Message message) {
        ensureConversation();
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString());
        }
        return conversation.addChild(parentId, message);
    }

    /** 重新生成：以指定消息为模板创建一个空兄弟节点并切换过去。 */
    public Message regenerate(String messageId) {
        ensureConversation();
        Message original = conversation.getMessage(messageId);
        if (original == null) {
            throw new IllegalArgumentException("message not found: " + messageId);
        }
        Message copy = new Message(original.getRole(), new ArrayList<>());
        copy.setId(UUID.randomUUID().toString());
        copy.setModelId(original.getModelId());
        return conversation.addChild(original.getParentId(), copy);
    }

    /** 编辑消息文本：覆盖第一个 RichText，没有则追加一个。 */
    public Message editMessage(String messageId, String newContent) {
        ensureConversation();
        Message message = conversation.getMessage(messageId);
        if (message == null) {
            throw new IllegalArgumentException("message not found: " + messageId);
        }
        for (Content content : message.getContents()) {
            if (content instanceof RichText) {
                content.overwrite(newContent);
                return message;
            }
        }
        message.getContents().add(new RichText(newContent));
        return message;
    }

    /** 删除节点，子节点归父。 */
    public void deleteMessage(String messageId) {
        ensureConversation();
        conversation.deleteMessage(messageId);
    }

    /** 获取从根到输入ID的路径。 */
    public List<Message> getPathFromRoot(String messageId) {
        ensureConversation();
        return conversation.getPathFromRoot(messageId);
    }

    /** 获取所有兄弟（不同版本）。 */
    public List<Message> getSiblings(String messageId) {
        ensureConversation();
        return conversation.getSiblings(messageId);
    }

    /** 切换活跃分支（输入 message 应为叶子节点）。 */
    public void switchBranch(String messageId) {
        ensureConversation();
        conversation.switchBranch(messageId);
    }

    private void ensureConversation() {
        if (conversation == null) {
            throw new IllegalStateException("conversation is null, call setConversation first");
        }
    }
}
