package org.bluepowerrobotics.letmeaskyou.core.adapter;

import org.bluepowerrobotics.converter.core.ChatModel;
import org.bluepowerrobotics.converter.provider.ChatModels;
import org.bluepowerrobotics.converter.provider.ProviderConfig;
import org.bluepowerrobotics.letmeaskyou.core.config.ModelConfig;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 提供商注册表 + 工厂。内置 OpenAI Chat / OpenAI Responses / Anthropic /
 * DashScope / Gemini 五种；未注册的 provider 按 OpenAI Chat Completions
 * 兼容端点处理（配合 ModelConfig.baseURL 指向任意兼容服务）。
 */
public final class AdapterManager {

    private static final Map<String, ApiAdapter> ADAPTERS = new LinkedHashMap<>();

    static {
        register(AdapterManager::openAiChat, "openai-chat", "openai", "chat");
        register(AdapterManager::openAiResponses, "openai-responses", "responses");
        register(AdapterManager::anthropic, "anthropic", "claude");
        register(AdapterManager::dashscope, "dashscope", "ali", "阿里");
        register(AdapterManager::gemini, "gemini", "google");
    }

    private AdapterManager() {
    }

    /** 注册自定义适配器。provider 匹配不区分大小写。 */
    public static void register(ApiAdapter adapter, String... aliases) {
        for (String alias : aliases) {
            ADAPTERS.put(normalize(alias), adapter);
        }
    }

    public static ApiAdapter getApiAdapter(String provider) {
        if (provider == null || provider.isEmpty()) {
            throw new IllegalArgumentException("provider must not be empty");
        }
        ApiAdapter adapter = ADAPTERS.get(normalize(provider));
        if (adapter != null) {
            return adapter;
        }
        // 未注册的提供商：按 OpenAI Chat Completions 兼容端点处理
        return AdapterManager::openAiChat;
    }

    /** 创建 ChatModel；provider 为空时抛异常，避免静默用错端点。 */
    public static ChatModel createChatModel(ModelConfig modelConfig) {
        Objects.requireNonNull(modelConfig, "modelConfig");
        return getApiAdapter(modelConfig.getProvider()).createChatModel(modelConfig);
    }

    private static ChatModel openAiChat(ModelConfig c) {
        return ChatModels.create(base(c, ProviderConfig.ProviderType.OPENAI_CHAT_COMPLETIONS));
    }

    private static ChatModel openAiResponses(ModelConfig c) {
        return ChatModels.create(base(c, ProviderConfig.ProviderType.OPENAI_RESPONSES));
    }

    private static ChatModel anthropic(ModelConfig c) {
        return ChatModels.create(base(c, ProviderConfig.ProviderType.ANTHROPIC));
    }

    private static ChatModel dashscope(ModelConfig c) {
        return ChatModels.create(base(c, ProviderConfig.ProviderType.DASHSCOPE));
    }

    private static ChatModel gemini(ModelConfig c) {
        return ChatModels.create(base(c, ProviderConfig.ProviderType.GEMINI));
    }

    private static ProviderConfig base(ModelConfig c, ProviderConfig.ProviderType type) {
        ProviderConfig.Builder b = ProviderConfig.builder()
                .type(type)
                .apiKey(c.getApiKey())
                .baseUrl(c.getBaseURL())
                .model(c.getModelName());
        return b.build();
    }

    private static String normalize(String provider) {
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
