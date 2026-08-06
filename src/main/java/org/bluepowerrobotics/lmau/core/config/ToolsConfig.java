package org.bluepowerrobotics.lmau.core.config;

import org.bluepowerrobotics.lmau.core.storage.ToolsStorage;
import org.bluepowerrobotics.lmau.core.toolcall.Tool;

import java.util.Collections;
import java.util.List;

/** 工具注册表访问入口，由 ToolsStorage 提供底层数据。 */
public class ToolsConfig {
    private final ToolsStorage toolsStorage;
    private static volatile ToolsConfig instance;

    private ToolsConfig(ToolsStorage toolsStorage) {
        this.toolsStorage = toolsStorage;
    }

    public static ToolsConfig setInstance(ToolsStorage toolsStorage) {
        instance = new ToolsConfig(toolsStorage);
        return instance;
    }

    public static ToolsConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ToolsConfig instance is null, call setInstance first.");
        }
        return instance;
    }

    public List<Tool> getTools() {
        return toolsStorage == null
                ? Collections.emptyList()
                : toolsStorage.getTools();
    }

    public Tool getTool(String toolName) {
        return toolsStorage == null ? null : toolsStorage.getTool(toolName);
    }
}
