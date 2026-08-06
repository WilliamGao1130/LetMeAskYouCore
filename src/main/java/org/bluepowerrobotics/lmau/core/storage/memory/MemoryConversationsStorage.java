package org.bluepowerrobotics.lmau.core.storage.memory;

import org.bluepowerrobotics.lmau.core.conversation.Conversation;
import org.bluepowerrobotics.lmau.core.storage.ConversationsStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 进程内对话存储（重启即失），作为平台实现（SQLite 等）的参考与测试用。 */
public class MemoryConversationsStorage implements ConversationsStorage {
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();

    @Override
    public Conversation getConversation(String conversationId) {
        return conversations.get(conversationId);
    }

    @Override
    public boolean ifConversationExists(String conversationId) {
        return conversations.containsKey(conversationId);
    }

    @Override
    public boolean saveConversation(Conversation conversation) {
        if (conversation == null || conversation.getId() == null) {
            return false;
        }
        conversations.put(conversation.getId(), conversation);
        return true;
    }

    @Override
    public List<Conversation> listConversations() {
        return new ArrayList<>(conversations.values());
    }

    @Override
    public List<String> listConversationsName() {
        List<String> names = new ArrayList<>();
        for (Conversation c : conversations.values()) {
            names.add(c.getTitle());
        }
        return names;
    }
}
