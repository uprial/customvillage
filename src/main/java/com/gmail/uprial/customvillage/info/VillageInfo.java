package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.CustomVillage;
import com.gmail.uprial.customvillage.common.CustomLogger;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.Cat;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;
import org.bukkit.util.Vector;

import java.util.*;

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

    private static final int PLAIN_MAP_SCALE = 8;
    private static final Vector CLUSTER_SCALE = new Vector(32, 5, 32);
    private static final int CLUSTER_SEARCH_DEPTH = 1;

    // https://minecraft.gamepedia.com/Cat
    private static final int MAX_CATS = 10;
    // https://minecraft.gamepedia.com/Iron_Golem
    private static final int VILLAGERS_PER_GOLEM = 4;

    private final CustomVillage plugin;
    private final CustomLogger customLogger;

    public VillageInfo(CustomVillage plugin,CustomLogger customLogger) {
        this.plugin = plugin;
        this.customLogger = customLogger;
    }

    private Map<Integer,Village> getVillagesMap(World world) {
        long startTime = 0;
        if(customLogger.isDebugMode()) {
            startTime = System.currentTimeMillis();
        }

        // Populate an aggregator
        final ClusterAggregator aggregator = new ClusterAggregator(world, CLUSTER_SCALE, CLUSTER_SEARCH_DEPTH);
        aggregator.populate(world.getEntitiesByClass(Villager.class));

        // Populate villagesMap
        Map<Integer,Village> villagesMap = new HashMap<>();
        for (Integer clusterId : aggregator.getAllClusterIds()) {
            final Village village = new Village();
            villagesMap.put(clusterId, village);

            aggregator.fetchBlocksInCluster(clusterId, (Block block) -> {
                if(block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                    org.bukkit.block.data.type.Bed bed = (org.bukkit.block.data.type.Bed)block.getBlockData();
                    if(bed.getPart() == Bed.Part.HEAD) {
                        village.bedHeads.add(block);
                    }
                }
            });
        }

        // Count villagers
        List<Villager> lostVillagers = aggregator.fetchEntities(Villager.class, (clusterId, villager) -> {
            Village village = villagesMap.get(clusterId);
            village.villagers.add(villager);
        });

        // Count ironGolems
        List<IronGolem> lostIronGolems = aggregator.fetchEntities(IronGolem.class, (clusterId, ironGolem) -> {
            if(!ironGolem.isPlayerCreated()) {
                Village village = villagesMap.get(clusterId);
                village.ironGolems.add(ironGolem);
            }
        });

        // Count cats
        List<Cat> lostCats = aggregator.fetchEntities(Cat.class, (clusterId, cat) -> {
            if(!cat.isLeashed()) {
                Village village = villagesMap.get(clusterId);
                village.cats.add(cat);
            }
        });

        if(customLogger.isDebugMode()) {
            long endTime = System.currentTimeMillis();
            customLogger.debug(String.format("Village info has been gathered in %dms.", (endTime - startTime)));
        }

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
            //final int clusterId = entry.getKey();
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

                Map<Integer,Village> villagesMap = getVillagesMap(world);
                for (Map.Entry<Integer,Village> entry : villagesMap.entrySet()) {
                    final Integer clusterId = entry.getKey();
                    final Village village = entry.getValue();

                    lines.add(String.format("== World '%s', village #%d ==", world.getName(), clusterId));
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
}
