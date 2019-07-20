package com.gmail.uprial.customvillage.crons;

import org.bukkit.scheduler.BukkitRunnable;

public class CronTask<T extends Runnable> extends BukkitRunnable {
    private final T tracker;

    CronTask(final T tracker) {
        this.tracker = tracker;
    }

    @Override
    public void run() {
        tracker.run();
    }
}