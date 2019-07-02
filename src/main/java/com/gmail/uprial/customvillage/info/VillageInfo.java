package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.storage.CustomStorage;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Cat;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

import static com.gmail.uprial.customvillage.common.Formatter.format;

public class VillageInfo {
    class Village {
        final List<Villager> villagers = new ArrayList<>();
        final List<IronGolem> ironGolems = new ArrayList<>();
        final List<Cat> cats = new ArrayList<>();
        final List<Block> bedHeads = new ArrayList<>();

        Village() {
        }
    }
    public interface Func {
        void call();
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

    public VillageInfo(CustomVillage plugin, CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    public void save() {
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                getStorage(world).save(getOrCreateAggregator(world).getDump());
            }
        }, (time) -> customLogger.debug(String.format("Village info has been saved in %dms.", time)));
    }

    public void update() {
        measureTime(() -> {
            for (World world : plugin.getServer().getWorlds()) {
                getVillages(world);
            }
        }, (time) -> customLogger.debug(String.format("Village info has been updated in %dms.", time)));
    }

    private CustomStorage getStorage(World world) {
        String filename = String.format("%s_villages.txt", world.getName());
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

    private Map<Integer,Village> getVillages(World world) {
        // Populate an aggregator
        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        aggregator.populate(world.getEntitiesByClass(Villager.class));

        // Populate villagesMap
        Map<Integer,Village> villagesMap = new HashMap<>();
        for (Integer villageId : aggregator.getAllClusterIds()) {
            final Village village = new Village();
            villagesMap.put(villageId, village);

            aggregator.fetchBlocksInCluster(villageId, (Block block) -> {
                if(block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                    org.bukkit.block.data.type.Bed bed = (org.bukkit.block.data.type.Bed)block.getBlockData();
                    if(bed.getPart() == Bed.Part.HEAD) {
                        village.bedHeads.add(block);
                    }
                }
            });
        }

        // Count villagers
        List<Villager> lostVillagers = aggregator.fetchEntities(Villager.class, (villageId, villager) -> {
            Village village = villagesMap.get(villageId);
            village.villagers.add(villager);
        });

        // Count ironGolems
        List<IronGolem> lostIronGolems = aggregator.fetchEntities(IronGolem.class, (villageId, ironGolem) -> {
            if(!ironGolem.isPlayerCreated()) {
                Village village = villagesMap.get(villageId);
                village.ironGolems.add(ironGolem);
            }
        });

        // Count cats
        List<Cat> lostCats = aggregator.fetchEntities(Cat.class, (villageId, cat) -> {
            if(!cat.isLeashed()) {
                Village village = villagesMap.get(villageId);
                village.cats.add(cat);
            }
        });

        for (Villager villager : lostVillagers) {
            customLogger.warning(String.format("Something went completely wrong and we lost a %s", format(villager)));
        }

        for (IronGolem ironGolem : lostIronGolems) {
            customLogger.warning(String.format("Removing a lost %s", format(ironGolem)));
        }

        for (Cat cat : lostCats) {
            customLogger.warning(String.format("Removing a lost %s", format(cat)));
        }

        for (Map.Entry<Integer,Village> entry : villagesMap.entrySet()) {
            //final int villageId = entry.getKey();
            final Village village = entry.getValue();

            if(village.villagers.size() > village.bedHeads.size()) {
                customLogger.warning(String.format("Only %d villager(s) have beds, the excessive %d villager(s) will be removed",
                        village.bedHeads.size(), village.villagers.size() - village.bedHeads.size()));
            }
            if(village.cats.size() > MAX_CATS) {
                customLogger.warning(String.format("Too many cats (>%d), the excessive %d cat(s) will be removed",
                        MAX_CATS, village.cats.size() - MAX_CATS));
            }
            if(village.ironGolems.size() > village.villagers.size() / VILLAGERS_PER_GOLEM) {
                customLogger.warning(String.format("Only %d iron golem(s) have support of villages, the excessive %d iron golem(s) will be removed",
                        village.villagers.size() / VILLAGERS_PER_GOLEM, village.ironGolems.size() - village.villagers.size() / VILLAGERS_PER_GOLEM));
            }
        }

        return villagesMap;
    }

    public List<String> getTextLines() {
        final List<String> lines = new ArrayList<>();
        for(World world : plugin.getServer().getWorlds()) {
            final Collection<Villager> entities = world.getEntitiesByClass(Villager.class);
            if(!entities.isEmpty()) {
                final PlainMapViewer viewer = new PlainMapViewer(PLAIN_MAP_SCALE);
                for (Villager entity : entities) {
                    Location location = entity.getLocation();
                    viewer.add(location.getBlockX(), location.getBlockZ());
                }
                lines.add(String.format("==== World '%s' ====", world.getName()));
                lines.addAll(viewer.getTextLines());

                Map<Integer,Village> villages = getVillages(world);
                for (Map.Entry<Integer,Village> entry : villages.entrySet()) {
                    final Integer villageId = entry.getKey();
                    final Village village = entry.getValue();

                    lines.add(String.format("== World '%s', village #%d ==", world.getName(), villageId));
                    lines.add(String.format("  Villagers: %d", village.villagers.size()));
                    lines.add(String.format("  Iron Golems: %d", village.ironGolems.size()));
                    lines.add(String.format("  Cats: %d", village.cats.size()));
                    lines.add(String.format("  Beds: %d", village.bedHeads.size()));
                    if(!village.villagers.isEmpty()) {
                        final PlainMapViewer villageViewer = new PlainMapViewer(PLAIN_MAP_SCALE);
                        for (Villager entity : village.villagers) {
                            villageViewer.add(entity.getLocation().getBlockX(), entity.getLocation().getBlockZ());
                        }
                        lines.addAll(villageViewer.getTextLines());
                    }
                }
            }
        }

        return lines;
    }

    private void measureTime(Func func, Consumer<Long> consumer) {
        long startTime = 0;
        if(customLogger.isDebugMode()) {
            startTime = System.currentTimeMillis();
        }
        func.call();
        if(customLogger.isDebugMode()) {
            long endTime = System.currentTimeMillis();
            consumer.accept(endTime - startTime);
        }
    }
}
