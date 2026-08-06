package org.bluepowerrobotics.lmau.core.storage;

import org.bluepowerrobotics.lmau.core.config.SettingsConfig;

public interface SettingsStorage {
    SettingsConfig getSettings();
    void setSettings(SettingsConfig settings);
}
