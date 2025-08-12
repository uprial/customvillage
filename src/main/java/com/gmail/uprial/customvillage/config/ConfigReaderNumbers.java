package com.gmail.uprial.customvillage.config;

import com.gmail.uprial.customvillage.common.CustomLogger;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigReaderNumbers {

    public static int getInt(FileConfiguration config, CustomLogger customLogger, String key, String title,
                             int min, int max) throws InvalidConfigException {
        return getIntInternal(config, customLogger, key, title, min, max, null);
    }

    public static int getInt(FileConfiguration config, CustomLogger customLogger, String key, String title,
                             int min, int max, @SuppressWarnings("SameParameterValue") int defaultValue) throws InvalidConfigException {
        return getIntInternal(config, customLogger, key, title, min, max, defaultValue);
    }

    private static int getIntInternal(FileConfiguration config, CustomLogger customLogger, String key, String title,
                                      int min, int max, Integer defaultValue) throws InvalidConfigException {
        if (min > max) {
            throw new InternalConfigurationError(String.format("Max value of %s is greater than max value", title));
        }

        Integer value = defaultValue;

        if(config.getString(key) == null) {
            if (defaultValue == null) {
                throw new InvalidConfigException(String.format("Empty %s", title));
            } else {
                customLogger.debug(String.format("Empty %s. Use default value %d", title, defaultValue));
            }
        } else if (! config.isInt(key)) {
            throw new InvalidConfigException(String.format("A %s is not an integer", title));
        } else {
            int intValue = config.getInt(key);

            if(min > intValue) {
                throw new InvalidConfigException(String.format("A %s should be at least %d", title, min));
            } else if(max < intValue) {
                throw new InvalidConfigException(String.format("A %s should be at most %d", title, max));
            } else {
                value = intValue;
            }
        }

        return value;
    }
}