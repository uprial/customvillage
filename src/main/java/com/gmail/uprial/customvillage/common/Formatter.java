package com.gmail.uprial.customvillage.common;

import org.bukkit.Location;
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
}
