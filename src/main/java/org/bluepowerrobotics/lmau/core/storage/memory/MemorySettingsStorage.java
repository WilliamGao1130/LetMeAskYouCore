package org.bluepowerrobotics.lmau.core.storage.memory;

import org.bluepowerrobotics.lmau.core.config.SettingsConfig;
import org.bluepowerrobotics.lmau.core.storage.SettingsStorage;

/** 进程内设置存储。 */
public class MemorySettingsStorage implements SettingsStorage {
    private SettingsConfig settings = new SettingsConfig();

    @Override
    public SettingsConfig getSettings() {
        return settings;
    }

    @Override
    public void setSettings(SettingsConfig settings) {
        this.settings = settings == null ? new SettingsConfig() : settings;
    }
}
