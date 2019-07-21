package com.gmail.uprial.customvillage.crons;

import com.gmail.uprial.customvillage.CustomVillage;

import static com.gmail.uprial.customvillage.common.Utils.SERVER_TICKS_IN_SECOND;

public class UpdateCron extends AbstractCron {
    private static final int INTERVAL = SERVER_TICKS_IN_SECOND * 60;

    private final CustomVillage plugin;

    public UpdateCron(CustomVillage plugin) {
        super(plugin, INTERVAL);

        this.plugin = plugin;

        onConfigChange();
    }

    @Override
    public void run() {
        plugin.updateInfo();
    }

    @Override
    boolean isEnabled() {
        return plugin.getCustomVillageConfig().isEnabled();
    }
}