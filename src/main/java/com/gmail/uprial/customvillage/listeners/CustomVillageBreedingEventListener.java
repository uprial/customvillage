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

import java.util.HashMap;
import java.util.Map;

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
        if(!event.isCancelled()) {
            // event.getEntity() will return an entity with zero coordinates which won't be applicable for the check.
            if (plugin.isEntityLimited(event.getMother(), CreatureSpawnEvent.SpawnReason.BREEDING)) {
                if(customLogger.isDebugMode()) {
                    // Breeding of VILLAGER[world: world, x: 0, y: 0, z: 0]
                    // with mother VILLAGER[world: world, x: -32, y: 65, z: 197]
                    // and father VILLAGER[world: world, x: -29, y: 65, z: 197] is not allowed
                    customLogger.debug(String.format("Breeding of %s with mother %s and father %s is not allowed",
                            format(event.getEntity()), format(event.getMother()), format(event.getFather())));
                }
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
        if(!event.isCancelled()) {
            final Entity entity = event.getEntity();
            if (plugin.isEntityLimited(entity, event.getSpawnReason())) {
                if(customLogger.isDebugMode()) {
                    // Spawn of CAT[world: world, x: -15, y: 65, z: 236] due to DEFAULT is not allowed
                    // Spawn of IRON_GOLEM[world: world, x: -26, y: 65, z: 203] due to VILLAGE_DEFENSE is not allowed
                    customLogger.debug(String.format("Spawn of %s due to %s is not allowed",
                            format(entity), event.getSpawnReason()));
                }
                event.setCancelled(true);
            }
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityTransformEvent(EntityTransformEvent event) {
        if(!event.isCancelled()) {
            final Entity sourceEntity = event.getEntity();
            for (final Entity targetEntity : event.getTransformedEntities()) {
                if (plugin.isEntityLimited(targetEntity, getSpawnReasonFromTransformReason(event.getTransformReason()))) {
                    if(customLogger.isDebugMode()) {
                        // Transformation of ZOMBIE_VILLAGER[world: world, x: -36, y: 61, z: 183]
                        // to VILLAGER[world: world, x: -36, y: 61, z: 183] due to CURED is not allowed
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

    private static final Map<EntityTransformEvent.TransformReason, CreatureSpawnEvent.SpawnReason> spawnReasonFromTransformReason
            = new HashMap<EntityTransformEvent.TransformReason, CreatureSpawnEvent.SpawnReason>() {{

                put(EntityTransformEvent.TransformReason.CURED, CreatureSpawnEvent.SpawnReason.CURED);
                put(EntityTransformEvent.TransformReason.INFECTION, CreatureSpawnEvent.SpawnReason.INFECTION);
                put(EntityTransformEvent.TransformReason.DROWNED, CreatureSpawnEvent.SpawnReason.DROWNED);
                put(EntityTransformEvent.TransformReason.SHEARED, CreatureSpawnEvent.SpawnReason.SHEARED);
                put(EntityTransformEvent.TransformReason.LIGHTNING, CreatureSpawnEvent.SpawnReason.LIGHTNING);
                put(EntityTransformEvent.TransformReason.SPLIT, CreatureSpawnEvent.SpawnReason.SLIME_SPLIT);
    }};
    private CreatureSpawnEvent.SpawnReason getSpawnReasonFromTransformReason(EntityTransformEvent.TransformReason transformReason) {
        CreatureSpawnEvent.SpawnReason spawnReason = spawnReasonFromTransformReason.get(transformReason);
        if(spawnReason == null) {
            spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
        }

        return spawnReason;
    }
}