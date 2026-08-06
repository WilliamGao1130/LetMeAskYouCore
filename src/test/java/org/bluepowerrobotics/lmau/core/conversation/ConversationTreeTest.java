package org.bluepowerrobotics.lmau.core.conversation;

import org.bluepowerrobotics.lmau.core.conversation.contents.Content;
import org.bluepowerrobotics.lmau.core.conversation.contents.RichText;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationTreeTest {

    private static Message message(String id, Message.Role role, String text) {
        Message m = new Message(role, Arrays.<Content>asList(new RichText(text)));
        m.setId(id);
        return m;
    }

    @Test
    void addChildBuildsTreeAndTracksCurrentLeaf() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "hi"));
        conversation.addChild("u1", message("m1", Message.Role.MODEL, "hello"));

        assertEquals("root", conversation.getRootMessageId());
        assertEquals("m1", conversation.getCurrentLeafId());
        assertEquals(3, conversation.getMessages().size());
        assertEquals("m1", conversation.getMessage("u1").getChildrenIds().get(0));
        assertEquals("u1", conversation.getMessage("m1").getParentId());

        List<Message> path = conversation.getPathFromRoot("m1");
        assertEquals(3, path.size());
        assertEquals("root", path.get(0).getId());
        assertEquals("m1", path.get(2).getId());
    }

    @Test
    void branchingDeactivatesSiblings() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "q1"));
        conversation.addChild("u1", message("a1", Message.Role.MODEL, "a1"));
        conversation.addChild("u1", message("a2", Message.Role.MODEL, "a2"));

        assertFalse(conversation.getMessage("a1").isActive());
        assertTrue(conversation.getMessage("a2").isActive());
        assertEquals("a2", conversation.getCurrentLeafId());

        List<Message> siblings = conversation.getSiblings("a1");
        assertEquals(1, siblings.size());
        assertEquals("a2", siblings.get(0).getId());
    }

    @Test
    void switchBranchMovesActiveLeaf() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "q1"));
        conversation.addChild("u1", message("a1", Message.Role.MODEL, "a1"));
        conversation.addChild("u1", message("a2", Message.Role.MODEL, "a2"));

        conversation.switchBranch("a1");

        assertTrue(conversation.getMessage("a1").isActive());
        assertFalse(conversation.getMessage("a2").isActive());
        assertEquals("a1", conversation.getCurrentLeafId());
    }

    @Test
    void deleteMessageMovesChildrenToParent() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "q1"));
        conversation.addChild("u1", message("a1", Message.Role.MODEL, "a1"));
        conversation.addChild("a1", message("u2", Message.Role.USER, "follow-up"));

        conversation.deleteMessage("a1");

        assertNull(conversation.getMessage("a1"));
        assertEquals("u1", conversation.getMessage("u2").getParentId());
        assertTrue(conversation.getMessage("u1").getChildrenIds().contains("u2"));
        assertEquals("u2", conversation.getCurrentLeafId());
    }

    @Test
    void deleteRootPromotesFirstChild() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "q1"));

        conversation.deleteMessage("root");

        assertEquals("u1", conversation.getRootMessageId());
        assertNull(conversation.getMessage("u1").getParentId());
    }

    @Test
    void regenerateCreatesSibling() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "q1"));
        conversation.addChild("u1", message("a1", Message.Role.MODEL, "a1"));

        ConversationController controller = new ConversationController(conversation);
        Message regenerated = controller.regenerate("a1");

        assertEquals("u1", regenerated.getParentId());
        assertEquals(1, conversation.getSiblings("a1").size());
        assertEquals(regenerated.getId(), conversation.getCurrentLeafId());
        assertFalse(conversation.getMessage("a1").isActive());
        assertTrue(regenerated.isActive());
        // 重新生成的节点应可追加内容（流式生成会写入）
        regenerated.getContents().add(new RichText("新回答"));
        assertEquals("新回答", regenerated.getContents().get(0).getStringContent());
    }

    @Test
    void editMessageOverwritesRichText() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        conversation.addChild("root", message("u1", Message.Role.USER, "old"));

        ConversationController controller = new ConversationController(conversation);
        controller.editMessage("u1", "new");

        assertEquals("new", conversation.getMessage("u1").getContents().get(0).getStringContent());
    }

    @Test
    void multipleRootsAreRejected() {
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, "sys"));
        assertThrows(IllegalStateException.class,
                () -> conversation.addChild(null, message("root2", Message.Role.USER, "x")));
    }
}
