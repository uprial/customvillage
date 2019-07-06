package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.storage.CustomStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.gmail.uprial.customvillage.common.Formatter.format;

public class VillageInfo {
    private static final int LOST_VILLAGE_ID = -1;
    private class Village {
        final List<Villager> villagers = new ArrayList<>();
        final List<IronGolem> ironGolems = new ArrayList<>();
        final List<Cat> cats = new ArrayList<>();
        final List<Block> bedHeads = new ArrayList<>();

        Village() {
        }
    }
    private class Villages extends HashMap<Integer,Village> {

    }
    public interface Func<T> {
        T call();
    }

    private static final int PLAIN_MAP_SCALE = 8;
    private static final Vector CLUSTER_SCALE = new Vector(32, 5, 32);
    private static final int CLUSTER_SEARCH_DEPTH = 1;

    // https://minecraft.gamepedia.com/Cat
    private static final int MAX_CATS = 10;
    // https://minecraft.gamepedia.com/Iron_Golem
    private static final int VILLAGERS_PER_GOLEM = 4;

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
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                getStorage(world).save(getOrCreateAggregator(world).getDump());
            }
            return null;
        }, (time) -> customLogger.debug(String.format("Village info has been saved in %dms.", time)));
    }

    public void update() {
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                getVillages(world);
            }
            return null;
        }, (time) -> customLogger.debug(String.format("Village info has been updated in %dms.", time)));
    }

    public void optimize() {
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                optimizeWorld(world);
            }
            return null;
        }, (time) -> customLogger.debug(String.format("Villages have been optimized in %dms.", time)));
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

    private <T extends Entity> void optimizeEntities(List<T> entities, int limit, Function<T,Boolean> function, String title) {
        int i = 0;
        while((i < entities.size() && (entities.size() > limit))) {
            final T entity = entities.get(i);
            if (function.apply(entity)) {
                customLogger.debug(String.format("Removing an excessive %s %s", title, format(entity)));
                entities.remove(i);
                entity.remove();
            } else {
                i++;
            }
        }
    }

    private <T extends Entity> void removeEntities(List<T> entities) {
        for (final T entity : entities) {
            customLogger.debug(String.format("Removing a lost %s", format(entity)));
            entity.remove();
        }
    }

    private void optimizeWorld(World world) {
        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        final Villages villages = getVillages(world);
        final Village lostVillage = villages.get(LOST_VILLAGE_ID);

        for (final Villager villager : lostVillage.villagers) {
            customLogger.warning(String.format("Something went completely wrong and we lost a %s", format(villager)));
        }

        removeEntities(lostVillage.ironGolems);
        removeEntities(lostVillage.cats);

        for (final Map.Entry<Integer,Village> entry : villages.entrySet()) {
            final Integer villageId = entry.getKey();
            if(villageId.equals(LOST_VILLAGE_ID)) {
                // do nothing
            } else if(!aggregator.isFullyLoaded(villageId)) {
                customLogger.debug(String.format("Village #%d is not fully loaded", villageId));
            } else {
                final Village village = entry.getValue();

                // Optimize villages
                int villagersLimit = village.bedHeads.size();
                if (village.villagers.size() > villagersLimit) {
                    customLogger.info(String.format("Only %d villager(s) have beds, the excessive %d villager(s) will be removed",
                            villagersLimit, village.villagers.size() - villagersLimit));

                    optimizeEntities(village.villagers, villagersLimit, (villager) -> !villager.isAdult(), "baby");
                    optimizeEntities(village.villagers, villagersLimit, (villager) -> villager.getProfession().equals(Villager.Profession.NITWIT), "nitwit");
                    optimizeEntities(village.villagers, villagersLimit, (villager) -> villager.getProfession().equals(Villager.Profession.NONE), "unemployed");
                    optimizeEntities(village.villagers, villagersLimit, (villager) -> true, "");
                }

                // Optimize vars
                if (village.cats.size() > MAX_CATS) {
                    customLogger.info(String.format("Too many cats (>%d), the excessive %d cat(s) will be removed",
                            MAX_CATS, village.cats.size() - MAX_CATS));

                    optimizeEntities(village.cats, MAX_CATS, (cat) -> !cat.isAdult(), "baby");
                    optimizeEntities(village.cats, MAX_CATS, (cat) -> true, "");
                }

                // Optimize iron goles
                int ironGolemsLimit = village.villagers.size() / VILLAGERS_PER_GOLEM;
                if (village.ironGolems.size() > village.villagers.size() / VILLAGERS_PER_GOLEM) {
                    customLogger.info(String.format("Only %d iron golem(s) have support of villages, the excessive %d iron golem(s) will be removed",
                            ironGolemsLimit, village.ironGolems.size() - ironGolemsLimit));

                    optimizeEntities(village.ironGolems, ironGolemsLimit, (ironGolem) -> true, "");
                }
            }
        }
    }

    public List<String> getTextLines() {
        return measureTime(() -> {
            final List<String> lines = new ArrayList<>();
            for (final World world : plugin.getServer().getWorlds()) {
                final Collection<Villager> entities = world.getEntitiesByClass(Villager.class);
                if (!entities.isEmpty()) {
                    lines.add(String.format("==== World '%s' ====", world.getName()));
                    lines.addAll(getViewTextLines(entities));

                    for (final Map.Entry<Integer, Village> entry : getVillages(world).entrySet()) {
                        final Integer villageId = entry.getKey();
                        if(!villageId.equals(LOST_VILLAGE_ID)) {
                            final Village village = entry.getValue();

                            lines.add(String.format("== World '%s', village #%d ==", world.getName(), villageId));
                            lines.add(String.format("  Villagers: %d", village.villagers.size()));
                            lines.add(String.format("  Iron Golems: %d", village.ironGolems.size()));
                            lines.add(String.format("  Cats: %d", village.cats.size()));
                            lines.add(String.format("  Beds: %d", village.bedHeads.size()));
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
