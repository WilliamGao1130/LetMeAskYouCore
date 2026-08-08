package org.bluepowerrobotics.lmau.core.storage;

import org.bluepowerrobotics.lmau.core.config.DisplayConfig;

public interface DisplayStorage {
    DisplayConfig getDisplayConfig();
    void setDisplayConfig(DisplayConfig displayConfig);
}
