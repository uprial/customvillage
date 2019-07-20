package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.storage.CustomStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.gmail.uprial.customvillage.common.Formatter.format;

public class VillageInfo {
    private static final int LOST_VILLAGE_ID = -1;

    private class Villages extends HashMap<Integer,Village> {
    }
    public interface Func<T> {
        T call();
    }

    private static final Vector CLUSTER_SCALE = new Vector(32, 5, 32);
    private static final int CLUSTER_SEARCH_DEPTH = 1;

    private final CustomVillage plugin;
    private final CustomLogger customLogger;

    private final Map<World,ClusterAggregator> aggregators = new HashMap<>();
    private final Map<World, Villages> worldVillages = new HashMap<>();

    public VillageInfo(CustomVillage plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
        update();
    }

    public void save() {
        forEachWorld((world -> getStorage(world).save(getOrCreateAggregator(world).getDump())),
                "Village info has been saved");
    }

    public void update() {
        forEachWorld((this::getVillages),
                "Village info has been updated");
    }

    public void optimize() {
        forEachWorld((this::optimizeWorld),
                "Villages have been optimized");
    }

    /*
        Possible input:

            VILLAGER, BREEDING | CURED
            IRON_GOLEM, VILLAGE_DEFENSE
            CAT, DEFAULT
     */
    private static final Map<EntityType,Set<CreatureSpawnEvent.SpawnReason>> entityTypesSpawnReasons = new HashMap<EntityType,Set<CreatureSpawnEvent.SpawnReason>>() {{
       put(EntityType.VILLAGER,
               new HashSet<CreatureSpawnEvent.SpawnReason>() {{
                   add(CreatureSpawnEvent.SpawnReason.BREEDING);
                   add(CreatureSpawnEvent.SpawnReason.CURED);
               }});
        put(EntityType.IRON_GOLEM,
                new HashSet<CreatureSpawnEvent.SpawnReason>() {{
                    add(CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE);
                }});
        put(EntityType.CAT,
                new HashSet<CreatureSpawnEvent.SpawnReason>() {{
                    add(CreatureSpawnEvent.SpawnReason.DEFAULT);
                }});
    }};

    public boolean isEntityLimited(final Entity entity, final CreatureSpawnEvent.SpawnReason spawnReason) {
        Set<CreatureSpawnEvent.SpawnReason> spawnReasons = entityTypesSpawnReasons.get(entity.getType());
        if(spawnReasons != null) {
            if(spawnReasons.contains(spawnReason)) {
                final ClusterAggregator aggregator = getOrCreateAggregator(entity.getWorld());
                final Integer villageId = aggregator.getEntityClusterId(entity);
                if(villageId == null) {
                    return true;
                }
                final Village village = getOrCreateVillages(entity.getWorld()).get(villageId);

                if(entity.getType().equals(EntityType.VILLAGER)) {
                    return village.getVillagers().size() >= village.getVillagersLimit();
                } else if(entity.getType().equals(EntityType.IRON_GOLEM)) {
                    return village.getIronGolems().size() >= village.getIronGolemsLimit();
                } else if(entity.getType().equals(EntityType.CAT)) {
                    return village.getCats().size() >= village.getCatsLimit();
                }
            }
        }

        return false;
    }

    public void onBlockChange(final Block block) {
        if(block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
            if(customLogger.isDebugMode()) {
                customLogger.debug(String.format("Block %s has been changed", format(block)));
            }
            final ClusterAggregator aggregator = getOrCreateAggregator(block.getWorld());
            aggregator.clearRegionBlocksCache(block);
        }
    }

    private void forEachWorld(final Consumer<World> consumer, final String whatHasBeenDone) {
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                consumer.accept(world);
            }
            return null;
        }, (time) -> customLogger.debug(String.format("%s in %dms.", whatHasBeenDone, time)));
    }

    private CustomStorage getStorage(final World world) {
        final String filename = String.format("%s_villages.txt", world.getName());
        return new CustomStorage(plugin.getDataFolder(), filename, customLogger);
    }

    private ClusterAggregator getOrCreateAggregator(final World world) {
        ClusterAggregator aggregator = aggregators.get(world);
        if(aggregator == null) {
            aggregator = new ClusterAggregator(world, CLUSTER_SCALE, CLUSTER_SEARCH_DEPTH);
            aggregators.put(world, aggregator);

            aggregator.loadFromDump(getStorage(world).load());
        }

        return aggregator;
    }

    private Villages getOrCreateVillages(final World world) {
        Villages villages = worldVillages.get(world);
        if (villages == null) {
            villages = new Villages();
            worldVillages.put(world, villages);
        }

        return villages;
    }

    private Villages getVillages(final World world) {
        // Populate an aggregator
        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        aggregator.populate(world.getEntitiesByClass(Villager.class));

        // Populate villages
        final Villages villages = getOrCreateVillages(world);
        villages.clear();
        for (final Integer villageId : aggregator.getAllClusterIds()) {
            final Village village = new Village();
            villages.put(villageId, village);

            village.getBedHeads().addAll(aggregator.getBlocksInCluster(villageId, (Block block) -> {
                if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                    final org.bukkit.block.data.type.Bed bed = (org.bukkit.block.data.type.Bed) block.getBlockData();
                    return (bed.getPart() == Bed.Part.HEAD);
                }
                return false;
            }));
        }

        final Village lostVillage = new Village();
        // Count villagers
        lostVillage.getVillagers().addAll(aggregator.fetchEntities(Villager.class, (villageId, villager) -> {
            Village village = villages.get(villageId);
            village.getVillagers().add(villager);
        }));

        // Count ironGolems
        lostVillage.getIronGolems().addAll(aggregator.fetchEntities(IronGolem.class, (villageId, ironGolem) -> {
            if (!ironGolem.isPlayerCreated()) {
                Village village = villages.get(villageId);
                village.getIronGolems().add(ironGolem);
            }
        }));

        // Count cats
        lostVillage.getCats().addAll(aggregator.fetchEntities(Cat.class, (villageId, cat) -> {
            if (!cat.isTamed()) {
                Village village = villages.get(villageId);
                village.getCats().add(cat);
            }
        }));

        villages.put(LOST_VILLAGE_ID, lostVillage);

        return villages;
    }

    private <T extends Entity> int optimizeExcessiveEntities(final List<T> entities, final int limit,
                                                             final Function<T,Boolean> toOptimize, String title) {
        int removed = 0;
        if(!title.isEmpty()) {
            title = " " + title;
        }

        int i = 0;
        while((i < entities.size() && (entities.size() > limit))) {
            final T entity = entities.get(i);
            if (toOptimize.apply(entity)) {
                customLogger.debug(String.format("Removing an excessive%s %s", title, format(entity)));
                entities.remove(i);
                entity.remove();
                removed++;
            } else {
                i++;
            }
        }

        return removed;
    }

    private <T extends Entity> int removeLostEntities(final List<T> entities, final String title) {
        int removed = 0;
        if(!entities.isEmpty()) {
            customLogger.info(String.format("The lost %d %s(s) will be removed", entities.size(), title));
            for (final T entity : entities) {
                customLogger.debug(String.format("Removing a lost %s", format(entity)));
                entity.remove();
                removed++;
            }
        }

        return removed;
    }

    private void optimizeWorld(final World world) {
        int tries = 3;
        while ((tries > 0) && (optimizeWorldOnce(world) > 0)) {
            tries--;
        }
    }

    private int optimizeWorldOnce(final World world) {
        int removed = 0;

        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        final Villages villages = getVillages(world);
        final Village lostVillage = villages.get(LOST_VILLAGE_ID);

        for (final Villager villager : lostVillage.getVillagers()) {
            customLogger.warning(String.format("Something went completely wrong and we lost a %s", format(villager)));
        }

        removed += removeLostEntities(lostVillage.getIronGolems(), "iron golem");
        removed += removeLostEntities(lostVillage.getCats(), "cat");

        for (final Map.Entry<Integer, Village> entry : villages.entrySet()) {
            final Integer villageId = entry.getKey();
            if (villageId.equals(LOST_VILLAGE_ID)) {
                // do nothing
            } else if (!aggregator.isFullyLoaded(villageId)) {
                customLogger.debug(String.format("Village #%d is not fully loaded", villageId));
            } else {
                final Village village = entry.getValue();

                // Optimize villages
                if (village.getVillagers().size() > village.getVillagersLimit()) {
                    customLogger.info(String.format("Only %d villager(s) in village #%d have beds, the excessive %d villager(s) will be removed",
                            village.getVillagersLimit(), villageId, village.getVillagers().size() - village.getVillagersLimit()));

                    removed += optimizeExcessiveEntities(village.getVillagers(), village.getVillagersLimit(),
                            (villager) -> !villager.isAdult(), "baby");
                    removed += optimizeExcessiveEntities(village.getVillagers(), village.getVillagersLimit(),
                            (villager) -> villager.getProfession().equals(Villager.Profession.NITWIT), "nitwit");
                    removed += optimizeExcessiveEntities(village.getVillagers(), village.getVillagersLimit(),
                            (villager) -> villager.getProfession().equals(Villager.Profession.NONE), "unemployed");
                    removed += optimizeExcessiveEntities(village.getVillagers(), village.getVillagersLimit(),
                            (villager) -> true, "");
                }

                // Optimize cats
                if (village.getCats().size() > village.getCatsLimit()) {
                    customLogger.info(String.format("Too many cats (>%d) in village #%d, the excessive %d cat(s) will be removed",
                            village.getCatsLimit(), villageId, village.getCats().size() - village.getCatsLimit()));

                    removed += optimizeExcessiveEntities(village.getCats(), village.getCatsLimit(), (cat) -> !cat.isAdult(), "baby");
                    removed += optimizeExcessiveEntities(village.getCats(), village.getCatsLimit(), (cat) -> true, "");
                }

                // Optimize iron golems
                if (village.getIronGolems().size() > village.getIronGolemsLimit()) {
                    customLogger.info(String.format("Only %d iron golem(s) in village #%d have support of villages, the excessive %d iron golem(s) will be removed",
                            village.getIronGolemsLimit(), villageId, village.getIronGolems().size() - village.getIronGolemsLimit()));

                    removed += optimizeExcessiveEntities(village.getIronGolems(), village.getIronGolemsLimit(), (ironGolem) -> true, "");
                }
            }
        }

        return removed;
    }

    public List<String> getTextLines(final VillageInfoType infoType, final Integer scale) {
        return measureTime(() -> {
            final List<String> lines = new ArrayList<>();
            for (final World world : plugin.getServer().getWorlds()) {
                Villages villages = getVillages(world);
                final Village lostVillage = villages.get(LOST_VILLAGE_ID);

                // villages.size() is always greater than 1 because of the LostVillage item.
                if((villages.size() > 1) || !lostVillage.getVillagers().isEmpty()
                        || !lostVillage.getIronGolems().isEmpty() || !lostVillage.getCats().isEmpty()) {
                    lines.add(String.format("==== World '%s' ====", world.getName()));
                    lines.add(String.format("Lost Villagers: %d", lostVillage.getVillagers().size()));
                    lines.add(String.format("Lost Iron Golems: %d", lostVillage.getIronGolems().size()));
                    lines.add(String.format("Lost Cats: %d", lostVillage.getCats().size()));
                    switch (infoType) {
                        case BEDS:
                            lines.add("/* Sorry, but all the beds can't be displayed");
                            lines.add("   due to performance reasons. */");
                            break;

                        case VILLAGERS:
                            lines.addAll(getViewTextLines(world.getEntitiesByClass(Villager.class), scale));
                            break;

                        case GOLEMS:
                            lines.addAll(getViewTextLines(world.getEntitiesByClass(IronGolem.class), scale));
                            break;

                        case CATS:
                            lines.addAll(getViewTextLines(world.getEntitiesByClass(Cat.class), scale));
                            break;
                    }


                    for (final Map.Entry<Integer, Village> entry : villages.entrySet()) {
                        final Integer villageId = entry.getKey();
                        if (!villageId.equals(LOST_VILLAGE_ID)) {
                            final Village village = entry.getValue();

                            lines.add(String.format("== World '%s', village #%d ==", world.getName(), villageId));
                            lines.add(String.format("  Beds: %d", village.getBedHeads().size()));
                            lines.add(String.format("  Villagers: %d/%d", village.getVillagers().size(), village.getVillagersLimit()));
                            lines.add(String.format("  Iron Golems: %d/%d", village.getIronGolems().size(), village.getIronGolemsLimit()));
                            lines.add(String.format("  Cats: %d/%d", village.getCats().size(), village.getCatsLimit()));
                            switch (infoType) {
                                case BEDS:
                                    final PlainMapViewer viewer = new PlainMapViewer(scale);
                                    for (final Block block : village.getBedHeads()) {
                                        final Location location = block.getLocation();
                                        viewer.add(location.getBlockX(), location.getBlockZ());
                                    }
                                    lines.addAll(viewer.getTextLines());
                                    break;

                                case VILLAGERS:
                                    lines.addAll(getViewTextLines(village.getVillagers(), scale));
                                    break;

                                case GOLEMS:
                                    lines.addAll(getViewTextLines(village.getIronGolems(), scale));
                                    break;

                                case CATS:
                                    lines.addAll(getViewTextLines(village.getCats(), scale));
                                    break;
                            }
                        }
                    }
                }
            }

            return lines;
        }, (time) -> customLogger.debug(String.format("Village info has been gathered in %dms.", time)));
    }

    private <T extends Entity> List<String> getViewTextLines(final Collection<T> entities, final int scale) {
        final PlainMapViewer viewer = new PlainMapViewer(scale);
        for (final Entity entity : entities) {
            final Location location = entity.getLocation();
            viewer.add(location.getBlockX(), location.getBlockZ());
        }
        return viewer.getTextLines();
    }

    private <T> T measureTime(final Func<T> func, final Consumer<Long> consumer) {
        long startTime = 0;
        if(customLogger.isDebugMode()) {
            startTime = System.currentTimeMillis();
        }
        final T result = func.call();
        if(customLogger.isDebugMode()) {
            final long endTime = System.currentTimeMillis();
            consumer.accept(endTime - startTime);
        }
        return result;
    }
}
