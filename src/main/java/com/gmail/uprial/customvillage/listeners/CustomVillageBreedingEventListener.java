package com.gmail.uprial.customvillage.listeners;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityTransformEvent;

import static com.gmail.uprial.customvillage.common.Formatter.format;

public class CustomVillageBreedingEventListener implements Listener {
    private final CustomVillage plugin;
    private final CustomLogger customLogger;

    public CustomVillageBreedingEventListener(CustomVillage plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityBreedEvent(EntityBreedEvent event) {
        if(!event.isCancelled() && plugin.isEnabled()) {
            if (!plugin.isEntityAllowed(event.getMother())) {
                customLogger.debug(String.format("Breeding of %s with mother %s and father %s is not allowed",
                        format(event.getEntity()), format(event.getMother()), format(event.getFather())));
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        if(!event.isCancelled() && plugin.isEnabled()) {
            Entity entity = event.getEntity();
            if (!plugin.isEntityAllowed(entity)) {
                customLogger.debug(String.format("Spawn of %s due to %s is not allowed",
                        format(entity), event.getSpawnReason()));
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTransformEvent(EntityTransformEvent event) {
        if(!event.isCancelled() && plugin.isEnabled()) {
            final Entity sourceEntity = event.getEntity();
            for (final Entity targetEntity : event.getTransformedEntities()) {
                if (!plugin.isEntityAllowed(targetEntity)) {
                    if(customLogger.isDebugMode()) {
                        customLogger.debug(String.format("Transformation of %s to %s due to %s is not allowed",
                                format(sourceEntity), format(targetEntity), event.getTransformReason()));
                    }
                    // If we don't kill or remove a zombie villager, it'll continue trying to transform.
                    if(sourceEntity instanceof LivingEntity) {
                        final LivingEntity sourceLivingEntity = (LivingEntity)sourceEntity;
                        final double maxHealth = sourceLivingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                        sourceLivingEntity.damage(maxHealth * 2.0);
                    } else {
                        sourceEntity.remove();
                    }
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }
}