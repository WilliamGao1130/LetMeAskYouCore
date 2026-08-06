package org.bluepowerrobotics.lmau.core.storage.memory;

import org.bluepowerrobotics.lmau.core.config.ModelConfig;
import org.bluepowerrobotics.lmau.core.storage.ModelsStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 进程内模型配置存储。 */
public class MemoryModelsStorage implements ModelsStorage {
    private final Map<String, ModelConfig> models = new ConcurrentHashMap<>();

    public void put(ModelConfig model) {
        if (model != null && model.getShowName() != null) {
            models.put(model.getShowName(), model);
        }
    }

    public void remove(String showName) {
        models.remove(showName);
    }

    @Override
    public List<ModelConfig> getModels() {
        return new ArrayList<>(models.values());
    }

    @Override
    public ModelConfig get(String showName) {
        return models.get(showName);
    }
}
