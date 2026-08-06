package org.bluepowerrobotics.lmau.core.storage.memory;

import org.bluepowerrobotics.lmau.core.storage.ToolsStorage;
import org.bluepowerrobotics.lmau.core.toolcall.Tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 进程内工具存储。 */
public class MemoryToolsStorage implements ToolsStorage {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        if (tool != null && tool.getName() != null) {
            tools.put(tool.getName(), tool);
        }
    }

    @Override
    public List<Tool> getTools() {
        return new ArrayList<>(tools.values());
    }

    @Override
    public Tool getTool(String toolName) {
        return tools.get(toolName);
    }
}
