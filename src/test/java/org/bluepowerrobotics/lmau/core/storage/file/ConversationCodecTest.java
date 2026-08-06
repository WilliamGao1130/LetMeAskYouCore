package org.bluepowerrobotics.lmau.core.storage.file;

import org.bluepowerrobotics.lmau.core.conversation.Conversation;
import org.bluepowerrobotics.lmau.core.conversation.Message;
import org.bluepowerrobotics.lmau.core.conversation.contents.Content;
import org.bluepowerrobotics.lmau.core.conversation.contents.PictureFile;
import org.bluepowerrobotics.lmau.core.conversation.contents.Reasoning;
import org.bluepowerrobotics.lmau.core.conversation.contents.RichText;
import org.bluepowerrobotics.lmau.core.conversation.contents.ToolCallContent;
import org.bluepowerrobotics.lmau.core.conversation.contents.ToolResult;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationCodecTest {

    @Test
    void roundTripPreservesTreeAndContents() throws Exception {
        Conversation conversation = new Conversation("s1", "标题");
        conversation.addChild(null, message("root", Message.Role.SYSTEM,
                new RichText("你是助手")));
        Message user = message("u1", Message.Role.USER,
                new RichText("看这张图"),
                new PictureFile("aGVsbG8=", "image/jpeg"));
        conversation.addChild("root", user);
        Message model = message("m1", Message.Role.MODEL,
                new RichText("让我查一下"),
                new Reasoning("先思考"),
                new ToolCallContent("call-1", "FetchUrl", "{\"url\":\"https://a.b\"}"));
        conversation.addChild("u1", model);
        Message toolResult = message("u2", Message.Role.USER,
                new ToolResult("call-1", "<html>ok</html>"));
        conversation.addChild("m1", toolResult);

        String json = ConversationCodec.toJson(conversation);
        Conversation restored = ConversationCodec.fromJson(json);

        assertEquals("s1", restored.getId());
        assertEquals("标题", restored.getTitle());
        assertEquals("u2", restored.getCurrentLeafId());
        assertEquals("root", restored.getRootMessageId());
        assertEquals(4, restored.getMessages().size());

        List<Message> path = restored.getPathFromRoot("u2");
        assertEquals(4, path.size());

        Message modelRestored = restored.getMessage("m1");
        assertTrue(modelRestored.getContents().get(0) instanceof RichText);
        assertTrue(modelRestored.getContents().get(1) instanceof Reasoning);
        ToolCallContent toolCall =
                (ToolCallContent) modelRestored.getContents().get(2);
        assertEquals("call-1", toolCall.getToolCallId());
        assertEquals("FetchUrl", toolCall.getName());

        Message userRestored = restored.getMessage("u1");
        PictureFile picture = (PictureFile) userRestored.getContents().get(1);
        assertEquals("image/jpeg", picture.getMimeType());
        assertEquals("aGVsbG8=", picture.getStringContent());

        ToolResult toolResultRestored =
                (ToolResult) restored.getMessage("u2").getContents().get(0);
        assertEquals("call-1", toolResultRestored.getToolCallId());
        assertEquals("<html>ok</html>", toolResultRestored.getStringContent());
        assertNotNull(toolResultRestored.getToolCallId());
    }

    @Test
    void readWriteUsesJavaIo() throws Exception {
        File dir = Files.createTempDirectory("codec-test").toFile();
        File file = new File(new File(dir, "nested"), "c.json");
        Conversation conversation = new Conversation("c1", "t");
        conversation.addChild(null, message("root", Message.Role.SYSTEM, new RichText("hi")));

        ConversationCodec.write(file, conversation);
        Conversation restored = ConversationCodec.read(file);

        assertEquals("c1", restored.getId());
        assertEquals("root", restored.getRootMessageId());
        assertEquals("hi", restored.getMessage("root").getContents().get(0).getStringContent());
    }

    private static Message message(String id, Message.Role role, Content... contents) {
        Message message = new Message(role, Arrays.asList(contents));
        message.setId(id);
        return message;
    }
}
