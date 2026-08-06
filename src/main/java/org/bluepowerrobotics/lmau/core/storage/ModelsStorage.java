package org.bluepowerrobotics.lmau.core.storage;

import org.bluepowerrobotics.lmau.core.config.ModelConfig;

import java.util.List;

public interface ModelsStorage {
    public List<ModelConfig> getModels();
    public ModelConfig get(String showName);
}
