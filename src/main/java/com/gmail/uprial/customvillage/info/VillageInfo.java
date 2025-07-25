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
import java.util.function.Supplier;

import static com.gmail.uprial.customvillage.common.Formatter.format;
import static com.gmail.uprial.customvillage.info.Village.isUserEntity;

public class VillageInfo {
    private static final int LOST_VILLAGE_ID = -1;

    class Villages extends HashMap<Integer,Village> {

        void callIfExists(final Integer villageId,
                          final LivingEntity entity,
                          final Consumer<Village> func) {
            final Village village = get(villageId);
            if(village != null) {
                func.accept(village);
            } else if (customLogger.isDebugMode()) {
                /*
                getVillages() considers Villages
                only when their chunks are at least partly loaded.

                Minecraft Server (I've no idea why!) keeps entities loaded
                when blocks in their chunks aren't fully loaded.

                So, there may be entities without Villages.

                Here is an example of debug info generated,
                with additional data in callIfExists():
                final Collection<Villager> villagers.

                customLogger.warning("==== BEGIN ====");
                for(final Villager villager : villagers) {
                    customLogger.warning(format(villager));
                }
                customLogger.warning(this.toString());

[12:59:29] [WARNING] Something went completely wrong and we gonna try to put VILLAGER[w: world, x: -297, y: 75, z: 1991, hp: 20.00, id: 399449a9-9a2e-40da-b3a4-ed9cebbd7ef1] into a village #14
==== BEGIN ====
VILLAGER[w: world, x: -297, y: 75, z: 1991, hp: 20.00, id: 399449a9-9a2e-40da-b3a4-ed9cebbd7ef1]
VILLAGER[w: world, x: -191, y: 62, z: 1342, hp: 11.00, id: d59d6153-e42b-4be7-bcdf-0580d060a07e]
VILLAGER[w: world, x: -182, y: 62, z: 1342, hp: 19.00, id: 5bfa84c0-f4c8-46ff-9efd-ecdfbbdf1832]
VILLAGER[w: world, x: -179, y: 62, z: 1342, hp: 20.00, id: 62340ead-9963-4f30-b36c-2dca77245523]
VILLAGER[w: world, x: -184, y: 62, z: 1341, hp: 13.00, id: 21260e35-302c-4af8-86a7-fb60c7187770]
VILLAGER[w: world, x: -178, y: 62, z: 1342, hp: 20.00, id: f0f77304-90b8-44cb-a1ef-0b7916c9ffd4]
VILLAGER[w: world, x: -188, y: 62, z: 1349, hp: 18.00, id: ed42a8e3-f2b6-479c-90cb-b551a0708174]
VILLAGER[w: world, x: -188, y: 54, z: 1346, hp: 14.00, id: 369b1c9f-f693-444e-943a-629515ce7773]
VILLAGER[w: world, x: -191, y: 62, z: 1348, hp: 18.00, id: a13b43d7-0bd5-4b94-9f67-57ae578008bd]
VILLAGER[w: world, x: -140, y: 62, z: 1346, hp: 20.00, id: 0b128e51-2cf0-48b5-afb6-2956d542ec6e]
VILLAGER[w: world, x: -140, y: 62, z: 1346, hp: 20.00, id: 93b8ead8-4531-46a7-a3b0-db3972167544]
VILLAGER[w: world, x: -164, y: 62, z: 1342, hp: 20.00, id: 00f3e2c5-1f7b-4dcf-af85-131e080e64f5]
VILLAGER[w: world, x: -168, y: 62, z: 1342, hp: 20.00, id: f0dfe240-8eeb-49de-8e21-ec7e2c7901a9]
VILLAGER[w: world, x: -175, y: 62, z: 1342, hp: 20.00, id: 58560de1-dbae-4245-9c19-aef5463df7e5]
VILLAGER[w: world, x: -195, y: 62, z: 1342, hp: 20.00, id: 086507b4-01b2-4d3a-874b-48e7c720c509]
VILLAGER[w: world, x: -142, y: 62, z: 1346, hp: 20.00, id: b39a1529-0a48-449d-b530-1c580f5b7e27]
VILLAGER[w: world, x: -193, y: 62, z: 1349, hp: 19.00, id: abbab2da-f93d-430f-a2c6-6217f81f936b]
VILLAGER[w: world, x: -195, y: 62, z: 1349, hp: 18.00, id: bd82ebb3-018b-449d-afc7-b36fba5959d9]
VILLAGER[w: world, x: -208, y: 47, z: 1270, hp: 19.00, id: 1545378c-8f0a-4b40-b08f-96215d0e2ba8]
VILLAGER[w: world, x: -208, y: 47, z: 1269, hp: 18.00, id: a452d383-6429-47a1-843b-42ffe2bc4675]
VILLAGER[w: world, x: -208, y: 47, z: 1269, hp: 20.00, id: 1d095d23-b54d-4b2a-a3c6-ccff16bf38bb]
{-1=Village{villagers: 0, bed-heads: 0, iron-golems{natural: 0, user: 0, all: 0}, cats{natural: 0, user: 0, all: 0}},
17=Village{villagers: 0, bed-heads: 17, iron-golems{natural: 0, user: 0, all: 0}, cats{natural: 0, user: 0, all: 0}},
18=Village{villagers: 0, bed-heads: 4, iron-golems{natural: 0, user: 0, all: 0}, cats{natural: 0, user: 0, all: 0}}}
                 */
                // The only purpose of passing entity is to write a good warning message.
                customLogger.debug(String.format(
                        "We try to put %s into a village #%d," +
                                " but it isn't loaded",
                        format(entity), villageId));
            }
        }
    }

    private static final Vector CLUSTER_SCALE = new Vector(32, 12, 32);
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
        if(isUserEntity(entity)) {
            return false;
        }

        Set<CreatureSpawnEvent.SpawnReason> spawnReasons = entityTypesSpawnReasons.get(entity.getType());
        if(spawnReasons != null) {
            if(spawnReasons.contains(spawnReason)) {
                final ClusterAggregator aggregator = getOrCreateAggregator(entity.getWorld());
                final Integer villageId = aggregator.getEntityClusterId(entity);
                if(villageId == null) {
                    if(spawnReason.equals(CreatureSpawnEvent.SpawnReason.CURED)) {
                        // Players can cure zombie-villagers to build a new village.
                        // Please refer to https://minecraft.gamepedia.com/Tutorials/Curing_a_zombie_villager.
                        //
                        // When a player tries to cure a zombie-villager and there are no other villagers nearby,
                        // a village can't be detected.
                        //
                        // The current condition allows the player to cure the first zombie-villager.
                        customLogger.debug(String.format("Curing of %s is allowed as an exception", format(entity)));
                        return false;
                    } else {
                        customLogger.debug(String.format("Spawn attempt of %s outside of any village", format(entity)));
                        return true;
                    }
                }
                final Village village = getOrCreateVillages(entity.getWorld()).get(villageId);
                if(village == null) {
                /*
[21:38:20 ERROR]: Could not pass event CreatureSpawnEvent to CustomVillage v0.1.10
java.lang.NullPointerException: Cannot invoke "com.gmail.uprial.customvillage.info.Village.getNaturalIronGolems()" because "village" is null
	at CustomVillage-0.1.10.jar/com.gmail.uprial.customvillage.info.VillageInfo.isEntityLimited(VillageInfo.java:123) ~[CustomVillage-0.1.10.jar:?]
	at CustomVillage-0.1.10.jar/com.gmail.uprial.customvillage.CustomVillage.isEntityLimited(CustomVillage.java:110) ~[CustomVillage-0.1.10.jar:?]
	at CustomVillage-0.1.10.jar/com.gmail.uprial.customvillage.listeners.CustomVillageBreedingEventListener.onCreatureSpawnEvent(CustomVillageBreedingEventListener.java:52) ~[CustomVillage-0.1.10.jar:?]
	at co.aikar.timings.TimedEventExecutor.execute(TimedEventExecutor.java:80) ~[paper-api-1.21.5-R0.1-SNAPSHOT.jar:?]
	at org.bukkit.plugin.RegisteredListener.callEvent(RegisteredListener.java:71) ~[paper-api-1.21.5-R0.1-SNAPSHOT.jar:?]
                 */
                    customLogger.debug(String.format("Spawn attempt of %s in a not yet even partially loaded cluster", format(entity)));
                    return true;
                }

                if(entity.getType().equals(EntityType.VILLAGER)) {
                    return village.getVillagers().size() >= village.getVillagersLimit();
                } else if(entity.getType().equals(EntityType.IRON_GOLEM)) {
                    return village.getNaturalIronGolems().size() >= village.getNaturalIronGolemsLimit();
                } else if(entity.getType().equals(EntityType.CAT)) {
                    return village.getNaturalCats().size() >= village.getNaturalCatsLimit();
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

    CustomStorage getStorage(final World world) {
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

    Villages getVillages(final World world) {
        // Populate an aggregator
        final ClusterAggregator aggregator = getOrCreateAggregator(world);
        aggregator.populate(world.getEntitiesByClass(Villager.class));

        // Populate villages
        final Villages villages = getOrCreateVillages(world);
        villages.clear();

        for (final Integer villageId : aggregator.getAllClusterIds()) {
            /*
                Counting bed heads in not yet even partially loaded clusters
                takes a lot of time on big maps:
                it loads or even generates (after map cleanup) unloaded map chunks.

                An experimental performance optimization:
                if a cluster is not yet even partially loaded,
                no reason to count bed heads.

                However, entities may be loaded in not yet even partially loaded clusters,
                which can be checked via "villages" existence.
            */
            if(aggregator.getClusterLoaded(villageId).equals(ClusterLoaded.NO)) {
                continue;
            }

            final Village village = new Village();
            villages.put(villageId, village);

            village.addAllBedHeads(aggregator.getBlocksInCluster(villageId, (Block block) -> {
                if (block.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                    final org.bukkit.block.data.type.Bed bed
                            = (org.bukkit.block.data.type.Bed) block.getBlockData();
                    return (bed.getPart() == Bed.Part.HEAD);
                }
                return false;
            }));
        }

        final Village lostVillage = new Village();
        villages.put(LOST_VILLAGE_ID, lostVillage);

        // Count villagers
        lostVillage.addAllVillagers(aggregator.fetchEntities(Villager.class, (villageId, villager) -> {
            villages.callIfExists(villageId, villager, (final Village village) -> village.addVillager(villager));
        }));

        // Count ironGolems
        lostVillage.addAllIronGolems(aggregator.fetchEntities(IronGolem.class, (villageId, ironGolem) -> {
            villages.callIfExists(villageId, ironGolem, (final Village village) -> village.addIronGolem(ironGolem));
        }));

        // Count cats
        lostVillage.addAllCats(aggregator.fetchEntities(Cat.class, (villageId, cat) -> {
            villages.callIfExists(villageId, cat, (final Village village) -> village.addCat(cat));
        }));

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
            entities.clear();
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
            somethingWentWrong(String.format("we lost a %s", format(villager)));
        }

        removed += removeLostEntities(lostVillage.getNaturalIronGolems(), "iron golem");
        removed += removeLostEntities(lostVillage.getNaturalCats(), "cat");

        for (final Map.Entry<Integer, Village> entry : villages.entrySet()) {
            final Integer villageId = entry.getKey();
            if (villageId.equals(LOST_VILLAGE_ID)) {
                // do nothing
            } else if (!aggregator.isClusterFullyLoaded(villageId)) {
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
                if (village.getNaturalCats().size() > village.getNaturalCatsLimit()) {
                    customLogger.info(String.format("Too many cats (>%d) in village #%d, the excessive %d cat(s) will be removed",
                            village.getNaturalCatsLimit(), villageId, village.getNaturalCats().size() - village.getNaturalCatsLimit()));

                    removed += optimizeExcessiveEntities(village.getNaturalCats(), village.getNaturalCatsLimit(), (cat) -> !cat.isAdult(), "baby");
                    removed += optimizeExcessiveEntities(village.getNaturalCats(), village.getNaturalCatsLimit(), (cat) -> true, "");
                }

                // Optimize iron golems
                if (village.getNaturalIronGolems().size() > village.getNaturalIronGolemsLimit()) {
                    customLogger.info(String.format("Only %d iron golem(s) in village #%d have support of villages, the excessive %d iron golem(s) will be removed",
                            village.getNaturalIronGolemsLimit(), villageId, village.getNaturalIronGolems().size() - village.getNaturalIronGolemsLimit()));

                    removed += optimizeExcessiveEntities(village.getNaturalIronGolems(), village.getNaturalIronGolemsLimit(), (ironGolem) -> true, "");
                }
            }
        }

        return removed;
    }

    public List<String> getTextLines(final VillageInfoType infoType, final ClusterLoaded clusterLoaded, final Integer scale) {
        return measureTime(() -> {
            final List<String> lines = new ArrayList<>();
            for (final World world : plugin.getServer().getWorlds()) {
                final ClusterAggregator aggregator = getOrCreateAggregator(world);
                final Villages villages = getVillages(world);
                final Village lostVillage = villages.get(LOST_VILLAGE_ID);

                // villages.size() is always greater than 1 because of the LostVillage item.
                if((villages.size() > 1) || !lostVillage.getVillagers().isEmpty()
                        || !lostVillage.getAllIronGolems().isEmpty() || !lostVillage.getAllCats().isEmpty()) {
                    lines.add(String.format("==== World '%s' ====", world.getName()));
                    lines.add(String.format("Lost Villagers: %d", lostVillage.getVillagers().size()));
                    lines.add(String.format("Lost Iron Golems: %d%s",
                            lostVillage.getNaturalIronGolems().size(), getUserOwnedMessage(lostVillage.getUserIronGolems().size())));
                    lines.add(String.format("Lost Cats: %d%s",
                            lostVillage.getNaturalCats().size(), getUserOwnedMessage(lostVillage.getUserCats().size())));
                    switch (infoType) {
                        case BEDS:
                            lines.add("/* Sorry, but all the loaded beds can't be displayed");
                            lines.add("   due to performance reasons. */");
                            break;

                        case VILLAGERS:
                            lines.add("Loaded Villagers:");
                            lines.addAll(getViewTextLines(world.getEntitiesByClass(Villager.class), scale));
                            break;

                        case GOLEMS:
                            lines.add("Loaded Iron Golems:");
                            lines.addAll(getViewTextLines(world.getEntitiesByClass(IronGolem.class), scale));
                            break;

                        case CATS:
                            lines.add("Loaded Cats:");
                            lines.addAll(getViewTextLines(world.getEntitiesByClass(Cat.class), scale));
                            break;
                    }


                    for (final Map.Entry<Integer, Village> entry : villages.entrySet()) {
                        final Integer villageId = entry.getKey();
                        if (!villageId.equals(LOST_VILLAGE_ID)) {
                            final Village village = entry.getValue();
                            final ClusterLoaded villageClusterLoaded = aggregator.getClusterLoaded(villageId);
                            if(
                                    ((clusterLoaded.equals(ClusterLoaded.FULLY)
                                            && !villageClusterLoaded.equals(ClusterLoaded.FULLY)))
                                ||
                                    ((clusterLoaded.equals(ClusterLoaded.PARTIALLY)
                                            && villageClusterLoaded.equals(ClusterLoaded.NO)))
                            ) {
                                continue;
                            }

                            lines.add(String.format("== World '%s', village #%d ==", world.getName(), villageId));
                            lines.add(String.format("  Beds: %d", village.getBedHeads().size()));
                            lines.add(String.format("  Villagers: %d/%d", village.getVillagers().size(), village.getVillagersLimit()));
                            lines.add(String.format("  Iron Golems: %d/%d%s",
                                    village.getNaturalIronGolems().size(), village.getNaturalIronGolemsLimit(),
                                    getUserOwnedMessage(village.getUserIronGolems().size())));
                            lines.add(String.format("  Cats: %d/%d%s",
                                    village.getNaturalCats().size(), village.getNaturalCatsLimit(),
                                    getUserOwnedMessage(village.getUserCats().size())));
                            lines.add("  Loaded: " + villageClusterLoaded);
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
                                    lines.addAll(getViewTextLines(village.getAllIronGolems(), scale));
                                    break;

                                case CATS:
                                    lines.addAll(getViewTextLines(village.getAllCats(), scale));
                                    break;
                            }
                        }
                    }
                }
            }

            if(lines.isEmpty()) {
                lines.add("No loaded villages.");
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

    private <T> T measureTime(final Supplier<T> func, final Consumer<Long> consumer) {
        long startTime = 0;
        if(customLogger.isDebugMode()) {
            startTime = System.currentTimeMillis();
        }
        final T result = func.get();
        if(customLogger.isDebugMode()) {
            final long endTime = System.currentTimeMillis();
            consumer.accept(endTime - startTime);
        }
        return result;
    }

    private String getUserOwnedMessage(int size) {
        if(size > 0) {
            return String.format(" (+%d user owned)", size);
        } else {
            return "";
        }
    }

    private void somethingWentWrong(final String message) {
        customLogger.warning(String.format("Something went completely wrong and %s", message));
    }
}
