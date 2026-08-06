package org.bluepowerrobotics.lmau.core.adapter;

import org.bluepowerrobotics.lmau.converter.core.ChatModel;
import org.bluepowerrobotics.lmau.converter.provider.anthropic.AnthropicChatModel;
import org.bluepowerrobotics.lmau.converter.provider.dashscope.DashScopeChatModel;
import org.bluepowerrobotics.lmau.converter.provider.gemini.GeminiChatModel;
import org.bluepowerrobotics.lmau.converter.provider.openai.OpenAIChatCompletionsModel;
import org.bluepowerrobotics.lmau.converter.provider.openai.OpenAIResponsesModel;
import org.bluepowerrobotics.lmau.core.config.ModelConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterManagerTest {

    private static ChatModel create(String provider) {
        ModelConfig config = new ModelConfig();
        config.setProvider(provider);
        config.setModelName("some-model");
        config.setApiKey("sk-test");
        return AdapterManager.createChatModel(config);
    }

    @Test
    void mapsBuiltInProviders() {
        assertTrue(create("openai-chat") instanceof OpenAIChatCompletionsModel);
        assertTrue(create("openai-responses") instanceof OpenAIResponsesModel);
        assertTrue(create("anthropic") instanceof AnthropicChatModel);
        assertTrue(create("dashscope") instanceof DashScopeChatModel);
        assertTrue(create("gemini") instanceof GeminiChatModel);
    }

    @Test
    void aliasesAndCaseInsensitive() {
        assertTrue(create("OpenAI") instanceof OpenAIChatCompletionsModel);
        assertTrue(create("Google") instanceof GeminiChatModel);
        assertTrue(create("Claude") instanceof AnthropicChatModel);
    }

    @Test
    void unknownProviderFallsBackToOpenAiCompatible() {
        assertTrue(create("custom") instanceof OpenAIChatCompletionsModel);
    }

    @Test
    void emptyProviderThrows() {
        assertThrows(IllegalArgumentException.class, () -> create(""));
    }
}
