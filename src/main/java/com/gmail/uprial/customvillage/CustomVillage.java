package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.config.InvalidConfigException;
import com.gmail.uprial.customvillage.info.VillageInfo;
import com.gmail.uprial.customvillage.listeners.CustomVillageBlocksListener;
import com.gmail.uprial.customvillage.listeners.CustomVillageBreedingEventListener;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.List;

import static com.gmail.uprial.customvillage.CustomVillageCommandExecutor.COMMAND_NS;

public final class CustomVillage extends JavaPlugin {
    private final String CONFIG_FILE_NAME = "config.yml";
    private final File configFile = new File(getDataFolder(), CONFIG_FILE_NAME);

    private CustomLogger consoleLogger = null;
    private CustomVillageConfig customVillageConfig = null;

    private VillageInfo villageInfo = null;

    private BukkitTask taskPeriodicSave;
    private BukkitTask taskPeriodicUpdate;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        consoleLogger = new CustomLogger(getLogger());
        customVillageConfig = loadConfig(getConfig(), consoleLogger);

        villageInfo = new VillageInfo(this, consoleLogger);

        taskPeriodicSave = new TaskPeriodicSave(this).runTaskTimer();
        taskPeriodicUpdate = new TaskPeriodicUpdate(this).runTaskTimer();
        getServer().getPluginManager().registerEvents(new CustomVillageBreedingEventListener(this, consoleLogger), this);
        getServer().getPluginManager().registerEvents(new CustomVillageBlocksListener(this), this);

        getCommand(COMMAND_NS).setExecutor(new CustomVillageCommandExecutor(this));
        consoleLogger.info("Plugin enabled");
    }

    void reloadConfig(CustomLogger userLogger) {
        reloadConfig();
        customVillageConfig = loadConfig(getConfig(), userLogger, consoleLogger);
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);

        taskPeriodicUpdate.cancel();
        taskPeriodicSave.cancel();

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

    List<String> getVillageInfoTextLines() {
        return villageInfo.getTextLines();
    }

    void saveInfo() {
        if(customVillageConfig.isEnabled()) {
            villageInfo.save();
        }
    }

    void updateInfo() {
        if(customVillageConfig.isEnabled()) {
            villageInfo.update();
        }
    }

    void optimize() {
        villageInfo.optimize();
    }

    public boolean isEntityLimited(Entity entity) {
        if(customVillageConfig.isEnabled()) {
            return villageInfo.isEntityLimited(entity);
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
