package com.gmail.uprial.customvillage;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.gmail.uprial.customvillage.common.Utils.SERVER_TICKS_IN_SECOND;

public class CustomVillagePlayerTracker extends BukkitRunnable {
    private static final int INTERVAL = SERVER_TICKS_IN_SECOND / 2;

    private final CustomVillage plugin;

    private static final Map<UUID, Map<Boolean, Location>> PLAYERS = new HashMap<>();
    private static boolean SIDE = true;

    public CustomVillagePlayerTracker(CustomVillage plugin) {
        this.plugin = plugin;
    }

    public BukkitTask runTaskTimer() {
        return runTaskTimer(plugin, INTERVAL, INTERVAL);
    }

    public static Vector getPlayerMovementVector(Player player) {
        UUID uuid = player.getUniqueId();
        Map<Boolean, Location> bucket = PLAYERS.get(uuid);
        if(bucket != null) {
            Location CurrentLocation = bucket.get(SIDE);
            Location OldLocation = bucket.get(!SIDE);
            if((CurrentLocation != null) || (OldLocation != null)) {
                return new Vector(
                        (CurrentLocation.getX() - OldLocation.getX()) / INTERVAL,
                        (CurrentLocation.getY() - OldLocation.getY()) / INTERVAL,
                        (CurrentLocation.getZ() - OldLocation.getZ()) / INTERVAL
                );
            }
        }
        return new Vector(0.0, 0.0, 0.0);
    }

    @Override
    public void run() {
        if(plugin.getCustomVillageConfig().isEnabled()){
            SIDE = !SIDE;
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                Map<Boolean, Location> bucket;
                if (PLAYERS.containsKey(uuid)) {
                    bucket = PLAYERS.get(uuid);
                } else {
                    bucket = new HashMap<>();
                    PLAYERS.put(uuid, bucket);
                }
                bucket.put(SIDE, player.getLocation());

            }
        }
    }
}