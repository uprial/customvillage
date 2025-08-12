package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.config.ConfigReaderNumbers;
import com.gmail.uprial.customvillage.config.ConfigReaderSimple;
import com.gmail.uprial.customvillage.config.InvalidConfigException;
import org.bukkit.configuration.file.FileConfiguration;

public final class CustomVillageConfig {
    private final boolean enabled;
    private final int timeoutInMs;

    private CustomVillageConfig(final boolean enabled,
                                final int timeoutInMs) {
        this.enabled = enabled;
        this.timeoutInMs = timeoutInMs;
    }

    static boolean isDebugMode(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        return ConfigReaderSimple.getBoolean(config, customLogger, "debug", "'debug' flag", false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getTimeoutInMs() {
        return timeoutInMs;
    }

    public static CustomVillageConfig getFromConfig(FileConfiguration config, CustomLogger customLogger) throws InvalidConfigException {
        boolean enabled = ConfigReaderSimple.getBoolean(config, customLogger, "enabled", "'enabled' flag", true);

        int timeoutInMs = ConfigReaderNumbers.getInt(config, customLogger, "timeout-in-ms", "'timeout-in-ms' value", 1, 3600_000, 5);

        return new CustomVillageConfig(enabled, timeoutInMs);
    }

    public String toString() {
        return String.format("enabled: %b, timeout-in-ms: %d",
                enabled, timeoutInMs);
    }
}
