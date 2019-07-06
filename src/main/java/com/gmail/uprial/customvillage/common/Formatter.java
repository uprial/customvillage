package com.gmail.uprial.customvillage.common;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public final class Formatter {
    public static String format(Entity entity) {
        if(entity == null) {
            return "null";
        }
        Location location = entity.getLocation();
        return String.format("%s[world: %s, x: %.0f, y: %.0f, z: %.0f]",
                entity.getType().toString(),
                (location.getWorld() != null) ? location.getWorld().getName() : "empty",
                location.getX(), location.getY(), location.getZ());
    }

    public static String format(Vector vector) {
        if(vector == null) {
            return "null";
        }
        return String.format("[x: %.2f, y: %.2f, z: %.2f, len: %.2f]",
                vector.getX(), vector.getY(), vector.getZ(), vector.length());
    }

    public static String format(Block block) {
        if(block == null) {
            return "null";
        }
        return String.format("%s[world: %s, x: %d, y: %d, z: %d]",
                block.getBlockData().getMaterial(),
                block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ());
    }
}
