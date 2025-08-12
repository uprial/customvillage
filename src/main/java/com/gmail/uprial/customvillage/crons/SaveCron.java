package com.gmail.uprial.customvillage.crons;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;

import static com.gmail.uprial.customvillage.common.Utils.SERVER_TICKS_IN_SECOND;

public class SaveCron extends AbstractCron {
    private static final int INTERVAL = SERVER_TICKS_IN_SECOND * 60 * 60;

    private final CustomVillage plugin;
    private final CustomLogger customLogger;
    private final int timeoutInMs;

    public SaveCron(final CustomVillage plugin,
                    final CustomLogger customLogger,
                    final int timeoutInMs) {
        super(plugin, INTERVAL);

        this.plugin = plugin;
        this.customLogger = customLogger;
        this.timeoutInMs = timeoutInMs;

        onConfigChange();
    }

    @Override
    public void run() {
        final long start = System.currentTimeMillis();
        plugin.saveInfo();
        final long end = System.currentTimeMillis();
        if(end - start >= timeoutInMs) {
            customLogger.warning(String.format("Save cron took %dms", end - start));
        }
    }

    @Override
    boolean isEnabled() {
        return plugin.getCustomVillageConfig().isEnabled();
    }
}