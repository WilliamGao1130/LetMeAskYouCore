package org.bluepowerrobotics.letmeaskyou.core.config;

/**
 * 一个可用模型的完整配置。
 * <p>
 * {@code provider} 取值与 AIAPIConverter 对齐：
 * {@code openai-chat} / {@code openai-responses} / {@code anthropic} /
 * {@code dashscope} / {@code gemini}；{@code custom} 表示任意 OpenAI Chat
 * Completions 兼容端点（配合 {@link #setBaseURL} 使用，如 DeepSeek、本地网关）。
 * </p>
 */
public class ModelConfig {
    private String provider;
    private String apiKey;
    private String modelName;
    private String baseURL;
    private long maxTokens;
    private String showName;

    public ModelConfig() {
    }

    public ModelConfig(String provider, String apiKey, String modelName,
                       String baseURL, long maxTokens, String showName) {
        this.provider = provider;
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseURL = baseURL;
        this.maxTokens = maxTokens;
        this.showName = showName;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public long getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(long maxTokens) {
        this.maxTokens = maxTokens;
    }

    public String getShowName() {
        return showName;
    }

    public void setShowName(String showName) {
        this.showName = showName;
    }

    @Override
    public String toString() {
        return "ModelConfig{provider='" + provider + "', model='" + modelName
                + "', showName='" + showName + "', baseURL='" + baseURL + "'}";
    }
}
