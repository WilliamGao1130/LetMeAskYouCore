package org.bluepowerrobotics.letmeaskyou.core.storage.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.bluepowerrobotics.letmeaskyou.core.conversation.Conversation;
import org.bluepowerrobotics.letmeaskyou.core.conversation.Message;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Content;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.PictureFile;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.Reasoning;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.RichText;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.TextFile;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.ToolCallContent;
import org.bluepowerrobotics.letmeaskyou.core.conversation.contents.ToolResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 对话树的 JSON 序列化/反序列化。
 * <p>
 * 只依赖 java.io（不依赖 java.nio.file），因此同时适用于桌面 CLI 与 Android。
 * </p>
 */
public final class ConversationCodec {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ConversationCodec() {
    }

    public static String toJson(Conversation conversation) throws IOException {
        ObjectNode root = JSON.createObjectNode();
        root.put("id", conversation.getId());
        root.put("title", conversation.getTitle());
        root.put("rootMessageId", conversation.getRootMessageId());
        root.put("currentLeafId", conversation.getCurrentLeafId());
        root.put("modelId", conversation.getModelId());
        root.put("createdAt", conversation.getCreatedAt());
        root.put("updatedAt", conversation.getUpdatedAt());
        ObjectNode messages = root.putObject("messages");
        for (Message message : conversation.getMessages().values()) {
            messages.set(message.getId(), messageToJson(message));
        }
        return JSON.writeValueAsString(root);
    }

    public static Conversation fromJson(String json) throws IOException {
        JsonNode root = JSON.readTree(json);
        Conversation conversation = new Conversation();
        conversation.setId(root.path("id").asText());
        conversation.setTitle(root.path("title").asText());
        conversation.setRootMessageId(nullableText(root, "rootMessageId"));
        conversation.setCurrentLeafId(nullableText(root, "currentLeafId"));
        conversation.setModelId(nullableText(root, "modelId"));
        conversation.setCreatedAt(root.path("createdAt").asLong());
        conversation.setUpdatedAt(root.path("updatedAt").asLong());
        JsonNode messages = root.path("messages");
        if (messages.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = messages.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                Message message = messageFromJson(entry.getValue());
                if (message.getId() != null) {
                    conversation.addMessage(message);
                }
            }
        }
        return conversation;
    }

    /** 从文件读取（自动创建父目录）。 */
    public static Conversation read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            return fromJson(new String(readAll(in), StandardCharsets.UTF_8));
        }
    }

    /** 写入文件（自动创建父目录）。 */
    public static void write(File file, Conversation conversation) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("无法创建目录: " + parent);
        }
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(toJson(conversation).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static ObjectNode messageToJson(Message message) {
        ObjectNode node = JSON.createObjectNode();
        node.put("id", message.getId());
        node.put("parentId", message.getParentId());
        node.put("conversationId", message.getConversationId());
        ArrayNode children = node.putArray("childrenIds");
        for (String childId : message.getChildrenIds()) {
            children.add(childId);
        }
        node.put("role", message.getRole().name());
        node.put("modelId", message.getModelId());
        node.put("timestamp", message.getTimestamp());
        node.put("active", message.isActive());
        node.put("finished", message.isFinished());
        ArrayNode contents = node.putArray("contents");
        for (Content content : message.getContents()) {
            contents.add(contentToJson(content));
        }
        return node;
    }

    private static Message messageFromJson(JsonNode node) {
        Message message = new Message();
        message.setId(node.path("id").asText(null));
        message.setParentId(nullableText(node, "parentId"));
        message.setConversationId(nullableText(node, "conversationId"));
        List<String> children = new ArrayList<>();
        for (JsonNode child : node.path("childrenIds")) {
            children.add(child.asText());
        }
        message.setChildrenIds(children);
        message.setRole(Message.Role.valueOf(node.path("role").asText("USER")));
        message.setModelId(nullableText(node, "modelId"));
        message.setTimestamp(node.path("timestamp").asLong());
        message.setActive(node.path("active").asBoolean(true));
        message.setFinished(node.path("finished").asBoolean(false));
        List<Content> contents = new ArrayList<>();
        for (JsonNode contentNode : node.path("contents")) {
            Content content = contentFromJson(contentNode);
            if (content != null) {
                contents.add(content);
            }
        }
        message.setContents(contents);
        return message;
    }

    private static ObjectNode contentToJson(Content content) {
        ObjectNode node = JSON.createObjectNode();
        if (content instanceof ToolCallContent) {
            ToolCallContent toolCall = (ToolCallContent) content;
            node.put("kind", "ToolCall");
            node.put("toolCallId", toolCall.getToolCallId());
            node.put("name", toolCall.getName());
            node.put("args", toolCall.getArgumentsJson());
        } else if (content instanceof ToolResult) {
            ToolResult toolResult = (ToolResult) content;
            node.put("kind", "ToolResult");
            node.put("toolCallId", toolResult.getToolCallId());
            node.put("text", toolResult.getStringContent());
        } else if (content instanceof PictureFile) {
            PictureFile picture = (PictureFile) content;
            node.put("kind", "PictureFile");
            node.put("text", picture.getStringContent());
            node.put("mime", picture.getMimeType());
        } else {
            node.put("kind", content.getKind());
            node.put("text", content.getStringContent());
        }
        return node;
    }

    private static Content contentFromJson(JsonNode node) {
        String kind = node.path("kind").asText("");
        String text = node.path("text").asText("");
        if ("ToolCall".equals(kind)) {
            return new ToolCallContent(
                    node.path("toolCallId").asText(null),
                    node.path("name").asText(""),
                    node.path("args").asText("{}"));
        }
        if ("ToolResult".equals(kind)) {
            return new ToolResult(node.path("toolCallId").asText(null), text);
        }
        if ("PictureFile".equals(kind)) {
            return new PictureFile(text, node.path("mime").asText("image/png"));
        }
        if ("RichText".equals(kind)) {
            return new RichText(text);
        }
        if ("Reasoning".equals(kind)) {
            return new Reasoning(text);
        }
        if ("TextFile".equals(kind)) {
            return new TextFile(text);
        }
        return null;
    }

    private static String nullableText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
