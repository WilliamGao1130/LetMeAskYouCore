package org.bluepowerrobotics.lmau.core.config;

import java.util.ArrayList;
import java.util.List;

/** 应用级设置（平台无关部分只定义数据结构，持久化交给 SettingsStorage）。 */
public class SettingsConfig {
    public enum Theme { LIGHT, DARK, SYSTEM }

    private String systemPrompt;
    private Theme theme = Theme.SYSTEM;
    private List<ModelConfig> modelConfigs = new ArrayList<>();

    public SettingsConfig() {
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public List<ModelConfig> getModelConfigs() {
        return modelConfigs;
    }

    public void setModelConfigs(List<ModelConfig> modelConfigs) {
        this.modelConfigs = modelConfigs == null ? new ArrayList<>() : modelConfigs;
    }

    public void addModelConfig(ModelConfig modelConfig) {
        if (modelConfig != null) {
            modelConfigs.add(modelConfig);
        }
    }
}
