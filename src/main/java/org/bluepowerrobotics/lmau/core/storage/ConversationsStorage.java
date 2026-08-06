package org.bluepowerrobotics.lmau.core.storage;

import org.bluepowerrobotics.lmau.core.conversation.Conversation;

import java.util.List;

/**
 * 对话记录持久化接口。
 * 负责 Conversation 和 Message 的 CRUD 以及树结构相关的原子操作。
 */
public interface ConversationsStorage {
    Conversation getConversation(String conversationId);
    boolean ifConversationExists(String conversationId);
    boolean saveConversation(Conversation conversation);
    List<Conversation> listConversations();
    List<String> listConversationsName();

}
