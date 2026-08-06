package org.bluepowerrobotics.lmau.core.storage;

import org.bluepowerrobotics.lmau.core.toolcall.Tool;

import java.util.List;

public interface ToolsStorage {
    public List<Tool> getTools();
    public Tool getTool(String toolName);
}
