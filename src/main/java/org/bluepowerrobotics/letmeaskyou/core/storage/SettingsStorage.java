package org.bluepowerrobotics.letmeaskyou.core.storage;

import org.bluepowerrobotics.letmeaskyou.core.config.SettingsConfig;

public interface SettingsStorage {
    SettingsConfig getSettings();
    void setSettings(SettingsConfig settings);
}
