package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.config.ConfigReaderSimple;
import com.gmail.uprial.customvillage.config.InvalidConfigException;
import org.bukkit.configuration.file.FileConfiguration;

public final class CustomVillageConfig {
    private final boolean enabled;

    private CustomVillageConfig(boolean enabled) {
        this.enabled = enabled;
    }

    static boolean isDebugMode(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        return ConfigReaderSimple.getBoolean(config, customLogger, "debug", "'debug' flag", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    static CustomVillageConfig getFromConfig(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        boolean enabled = ConfigReaderSimple.getBoolean(config, customLogger, "enabled", "'enabled' flag", true);

        return new CustomVillageConfig(enabled);
    }

    public String toString() {
        return String.format("enabled: %b", enabled);
    }
}
