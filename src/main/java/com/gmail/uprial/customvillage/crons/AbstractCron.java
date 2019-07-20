package com.gmail.uprial.customvillage.crons;

import com.gmail.uprial.customvillage.CustomVillage;

abstract class AbstractCron implements Runnable {
    private final CustomVillage plugin;
    private final int interval;

    private CronTask<AbstractCron> task;

    private boolean enabled = false;

    AbstractCron(final CustomVillage plugin, final int interval) {
        this.plugin = plugin;
        this.interval = interval;
    }

    public void stop() {
        setEnabled(false);
    }

    public void onConfigChange() {
        setEnabled(isEnabled());
    }

    private boolean isEnabled() {
        return plugin.getCustomVillageConfig().isEnabled();
    }

    private void setEnabled(final boolean enabled) {
        if(this.enabled != enabled) {
            if (enabled) {
                task = new CronTask<>(this);
                task.runTaskTimer(plugin, interval, interval);
            } else {
                task.cancel();
                task = null;
            }

            this.enabled = enabled;
        }
    }
}
