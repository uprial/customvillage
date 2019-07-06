package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.storage.CustomStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
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

    private static final int PLAIN_MAP_SCALE = 8;
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

    private static final Set<EntityType> interestingEntityTypes = new HashSet<EntityType>() {{
       add(EntityType.VILLAGER);
       add(EntityType.IRON_GOLEM);
       add(EntityType.CAT);
    }};

    public boolean isEntityLimited(Entity entity) {
        if(interestingEntityTypes.contains(entity.getType())) {
            final ClusterAggregator aggregator = getOrCreateAggregator(entity.getWorld());
            final Integer villageId = aggregator.getEntityClusterId(entity);
            if(villageId == null) {
                // This is a pretty questionable approach, to restrict spawn in areas not considered as villages.
                // But otherwise, cats and iron golems will spread outside of the village.
                return true;
            } else {
                final Village village = getOrCreateVillages(entity.getWorld()).get(villageId);
                if(entity.getType().equals(EntityType.VILLAGER)) {
                    return village.villagers.size() >= village.getVillagersLimit();
                } else if(entity.getType().equals(EntityType.IRON_GOLEM)) {
                    return village.ironGolems.size() >= village.getIronGolemsLimit();
                } else if(entity.getType().equals(EntityType.CAT)) {
                    return village.cats.size() >= village.getCatsLimit();
                }
            }
        }

        return false;
    }

    private void forEachWorld(Consumer<World> consumer, String whatHasBeenDone) {
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                consumer.accept(world);
            }
            return null;
        }, (time) -> customLogger.debug(String.format("%s in %dms.", whatHasBeenDone, time)));
    }

    private CustomStorage getStorage(World world) {
        final String filename = String.format("%s_villages.txt", world.getName());
        return new CustomStorage(plugin.getDataFolder(), filename, customLogger);
    }

    private ClusterAggregator getOrCreateAggregator(World world) {
        ClusterAggregator aggregator = aggregators.get(world);
        if(aggregator == null) {
            aggregator = new ClusterAggregator(world, CLUSTER_SCALE, CLUSTER_SEARCH_DEPTH);
            aggregators.put(world, aggregator);

            aggregator.loadFromDump(getStorage(world).load());
        }

        return aggregator;
    }

    private Villages getOrCreateVillages(World world) {
        Villages villages = worldVillages.get(world);
        if (villages == null) {
            villages = new Villages();
            worldVillages.put(world, villages);
        }

        return villages;
    }

    private Villages getVillages(World world) {
        // Populate an aggregator
        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        aggregator.populate(world.getEntitiesByClass(Villager.class));

        // Populate villages
        final Villages villages = getOrCreateVillages(world);
        villages.clear();
        for (final Integer villageId : aggregator.getAllClusterIds()) {
            final Village village = new Village();
            villages.put(villageId, village);

            aggregator.fetchBlocksInCluster(villageId, (Block block) -> {
                if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                    final org.bukkit.block.data.type.Bed bed = (org.bukkit.block.data.type.Bed) block.getBlockData();
                    if (bed.getPart() == Bed.Part.HEAD) {
                        village.bedHeads.add(block);
                    }
                }
            });
        }

        final Village lostVillage = new Village();
        // Count villagers
        lostVillage.villagers.addAll(aggregator.fetchEntities(Villager.class, (villageId, villager) -> {
            Village village = villages.get(villageId);
            village.villagers.add(villager);
        }));

        // Count ironGolems
        lostVillage.ironGolems.addAll(aggregator.fetchEntities(IronGolem.class, (villageId, ironGolem) -> {
            if (!ironGolem.isPlayerCreated()) {
                Village village = villages.get(villageId);
                village.ironGolems.add(ironGolem);
            }
        }));

        // Count cats
        lostVillage.cats.addAll(aggregator.fetchEntities(Cat.class, (villageId, cat) -> {
            if (!cat.isTamed()) {
                Village village = villages.get(villageId);
                village.cats.add(cat);
            }
        }));

        villages.put(LOST_VILLAGE_ID, lostVillage);

        return villages;
    }

    private <T extends Entity> int optimizeEntities(final List<T> entities, final int limit,
                                                     final Function<T,Boolean> function, String title) {
        int removed = 0;
        if(!title.isEmpty()) {
            title = " " + title;
        }

        int i = 0;
        while((i < entities.size() && (entities.size() > limit))) {
            final T entity = entities.get(i);
            if (function.apply(entity)) {
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

    private <T extends Entity> int removeEntities(List<T> entities) {
        int removed = 0;
        for (final T entity : entities) {
            customLogger.debug(String.format("Removing a lost %s", format(entity)));
            entity.remove();
            removed++;
        }

        return removed;
    }

    private void optimizeWorld(World world) {
        int tries = 3;
        while ((tries > 0) && (optimizeWorldOnce(world) > 0)) {
            tries--;
        }
    }

    private int optimizeWorldOnce(World world) {
        int removed = 0;

        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        final Villages villages = getVillages(world);
        final Village lostVillage = villages.get(LOST_VILLAGE_ID);

        for (final Villager villager : lostVillage.villagers) {
            customLogger.warning(String.format("Something went completely wrong and we lost a %s", format(villager)));
        }

        removed += removeEntities(lostVillage.ironGolems);
        removed += removeEntities(lostVillage.cats);

        for (final Map.Entry<Integer, Village> entry : villages.entrySet()) {
            final Integer villageId = entry.getKey();
            if (villageId.equals(LOST_VILLAGE_ID)) {
                // do nothing
            } else if (!aggregator.isFullyLoaded(villageId)) {
                customLogger.debug(String.format("Village #%d is not fully loaded", villageId));
            } else {
                final Village village = entry.getValue();

                // Optimize villages
                if (village.villagers.size() > village.getVillagersLimit()) {
                    customLogger.info(String.format("Only %d villager(s) in village #%d have beds, the excessive %d villager(s) will be removed",
                            village.getVillagersLimit(), villageId, village.villagers.size() - village.getVillagersLimit()));

                    removed += optimizeEntities(village.villagers, village.getVillagersLimit(),
                            (villager) -> !villager.isAdult(), "baby");
                    removed += optimizeEntities(village.villagers, village.getVillagersLimit(),
                            (villager) -> villager.getProfession().equals(Villager.Profession.NITWIT), "nitwit");
                    removed += optimizeEntities(village.villagers, village.getVillagersLimit(),
                            (villager) -> villager.getProfession().equals(Villager.Profession.NONE), "unemployed");
                    removed += optimizeEntities(village.villagers, village.getVillagersLimit(),
                            (villager) -> true, "");
                }

                // Optimize cats
                if (village.cats.size() > village.getCatsLimit()) {
                    customLogger.info(String.format("Too many cats (>%d) in village #%d, the excessive %d cat(s) will be removed",
                            village.getCatsLimit(), villageId, village.cats.size() - village.getCatsLimit()));

                    removed += optimizeEntities(village.cats, village.getCatsLimit(), (cat) -> !cat.isAdult(), "baby");
                    removed += optimizeEntities(village.cats, village.getCatsLimit(), (cat) -> true, "");
                }

                // Optimize iron golems
                if (village.ironGolems.size() > village.getIronGolemsLimit()) {
                    customLogger.info(String.format("Only %d iron golem(s) in village #%d have support of villages, the excessive %d iron golem(s) will be removed",
                            village.getIronGolemsLimit(), villageId, village.ironGolems.size() - village.getIronGolemsLimit()));

                    removed += optimizeEntities(village.ironGolems, village.getIronGolemsLimit(), (ironGolem) -> true, "");
                }
            }
        }

        return removed;
    }

    public List<String> getTextLines() {
        return measureTime(() -> {
            final List<String> lines = new ArrayList<>();
            for (final World world : plugin.getServer().getWorlds()) {
                final Collection<Villager> entities = world.getEntitiesByClass(Villager.class);
                if (!entities.isEmpty()) {
                    lines.add(String.format("==== World '%s' ====", world.getName()));
                    Villages villages = getVillages(world);

                    final Village lostVillage = villages.get(LOST_VILLAGE_ID);
                    lines.add(String.format("Lost Villagers: %d", lostVillage.villagers.size()));
                    lines.add(String.format("Lost Iron Golems: %d", lostVillage.ironGolems.size()));
                    lines.add(String.format("Lost Cats: %d", lostVillage.cats.size()));
                    lines.addAll(getViewTextLines(entities));

                    for (final Map.Entry<Integer, Village> entry : villages.entrySet()) {
                        final Integer villageId = entry.getKey();
                        if(!villageId.equals(LOST_VILLAGE_ID)) {
                            final Village village = entry.getValue();

                            lines.add(String.format("== World '%s', village #%d ==", world.getName(), villageId));
                            lines.add(String.format("  Beds: %d", village.bedHeads.size()));
                            lines.add(String.format("  Villagers: %d/%d", village.villagers.size(), village.getVillagersLimit()));
                            lines.add(String.format("  Iron Golems: %d/%d", village.ironGolems.size(), village.getIronGolemsLimit()));
                            lines.add(String.format("  Cats: %d/%d", village.cats.size(), village.getCatsLimit()));
                            if (!village.villagers.isEmpty()) {
                                lines.addAll(getViewTextLines(village.villagers));
                            }
                        }
                    }
                }
            }

            return lines;
        }, (time) -> customLogger.debug(String.format("Village info has been gathered in %dms.", time)));
    }

    private <T extends Entity> List<String> getViewTextLines(Collection<T> entities) {
        final PlainMapViewer viewer = new PlainMapViewer(PLAIN_MAP_SCALE);
        for (final Entity entity : entities) {
            final Location location = entity.getLocation();
            viewer.add(location.getBlockX(), location.getBlockZ());
        }
        return viewer.getTextLines();
    }

    private <T> T measureTime(Func<T> func, Consumer<Long> consumer) {
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
