package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.config.InvalidConfigException;
import com.gmail.uprial.customvillage.crons.SaveCron;
import com.gmail.uprial.customvillage.crons.UpdateCron;
import com.gmail.uprial.customvillage.info.VillageInfo;
import com.gmail.uprial.customvillage.info.VillageInfoType;
import com.gmail.uprial.customvillage.listeners.CustomVillageBlocksListener;
import com.gmail.uprial.customvillage.listeners.CustomVillageBreedingEventListener;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

import static com.gmail.uprial.customvillage.CustomVillageCommandExecutor.COMMAND_NS;

public final class CustomVillage extends JavaPlugin {
    private final String CONFIG_FILE_NAME = "config.yml";
    private final File configFile = new File(getDataFolder(), CONFIG_FILE_NAME);

    private CustomLogger consoleLogger = null;
    private CustomVillageConfig customVillageConfig = null;

    private VillageInfo villageInfo = null;

    private SaveCron saveCron;
    private UpdateCron updateCron;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        consoleLogger = new CustomLogger(getLogger());
        customVillageConfig = loadConfig(getConfig(), consoleLogger);

        villageInfo = new VillageInfo(this, consoleLogger);

        saveCron = new SaveCron(this);
        updateCron = new UpdateCron(this);
        getServer().getPluginManager().registerEvents(new CustomVillageBreedingEventListener(this, consoleLogger), this);
        getServer().getPluginManager().registerEvents(new CustomVillageBlocksListener(this), this);

        getCommand(COMMAND_NS).setExecutor(new CustomVillageCommandExecutor(this));
        consoleLogger.info("Plugin enabled");
    }

    public CustomVillageConfig getCustomVillageConfig() {
        return customVillageConfig;
    }

    void reloadConfig(CustomLogger userLogger) {
        reloadConfig();
        customVillageConfig = loadConfig(getConfig(), userLogger, consoleLogger);
        saveCron.onConfigChange();
        updateCron.onConfigChange();
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        updateCron.stop();
        saveCron.stop();

        saveInfo();

        consoleLogger.info("Plugin disabled");
    }

    @Override
    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            saveResource(CONFIG_FILE_NAME, false);
        }
    }

    @Override
    public FileConfiguration getConfig() {
        return YamlConfiguration.loadConfiguration(configFile);
    }

    List<String> getVillageInfoTextLines(final VillageInfoType infoType, final Integer scale) {
        return villageInfo.getTextLines(infoType, scale);
    }

    public void saveInfo() {
        // customVillageConfig.isEnabled() is checked in the cron task
        villageInfo.save();
    }

    public void updateInfo() {
        // customVillageConfig.isEnabled() is checked in the cron task
        villageInfo.update();
    }

    void optimize() {
        villageInfo.optimize();
    }

    public boolean isEntityLimited(Entity entity, CreatureSpawnEvent.SpawnReason spawnReason) {
        if(customVillageConfig.isEnabled()) {
            return villageInfo.isEntityLimited(entity, spawnReason);
        } else {
            return false;
        }
    }

    public void onBlockChange(Block block) {
        if(customVillageConfig.isEnabled()) {
            villageInfo.onBlockChange(block);
        }
    }

    private static CustomVillageConfig loadConfig(FileConfiguration config, CustomLogger customLogger) {
        return loadConfig(config, customLogger, null);
    }

    private static CustomVillageConfig loadConfig(FileConfiguration config, CustomLogger mainLogger, CustomLogger secondLogger) {
        CustomVillageConfig customVillageConfig = null;
        try {
            boolean isDebugMode = CustomVillageConfig.isDebugMode(config, mainLogger);
            mainLogger.setDebugMode(isDebugMode);
            if(secondLogger != null) {
                secondLogger.setDebugMode(isDebugMode);
            }

            customVillageConfig = CustomVillageConfig.getFromConfig(config, mainLogger);
        } catch (InvalidConfigException e) {
            mainLogger.error(e.getMessage());
        }

        return customVillageConfig;
    }
}
