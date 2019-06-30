package com.gmail.uprial.customvillage.common;

import com.gmail.uprial.customvillage.CustomVillage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public final class MetadataHelper {
    public static void setMetadata(CustomVillage plugin, LivingEntity entity, String key, Object value) {
        MetadataValue metadataValue = new FixedMetadataValue(plugin, value);
        entity.setMetadata(key, metadataValue);
    }

    public static <T> T getMetadata(LivingEntity entity, String key) {
        MetadataValue metadataValue = getMetadataValue(entity, key);
        //noinspection unchecked
        return (metadataValue != null) ? (T) metadataValue.value() : null;
    }

    private static MetadataValue getMetadataValue(LivingEntity entity, String key) {
        if (entity.hasMetadata(key)) {
            List<MetadataValue> metadataValues = entity.getMetadata(key);
            if (!metadataValues.isEmpty()) {
                return metadataValues.get(0);
            }
        }

        return null;
    }
}
