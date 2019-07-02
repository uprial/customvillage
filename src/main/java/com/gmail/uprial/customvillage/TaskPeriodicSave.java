package com.gmail.uprial.customvillage;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static com.gmail.uprial.customvillage.common.Utils.SERVER_TICKS_IN_SECOND;

class TaskPeriodicSave extends BukkitRunnable {
    private static final int INTERVAL = SERVER_TICKS_IN_SECOND * 60 * 5;

    private final CustomVillage plugin;

    TaskPeriodicSave(CustomVillage plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        plugin.saveInfo();
    }

    public BukkitTask runTaskTimer() {
        return runTaskTimer(plugin, INTERVAL, INTERVAL);
    }

}