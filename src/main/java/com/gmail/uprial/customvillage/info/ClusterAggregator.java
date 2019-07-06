package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.storage.StorageData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class ClusterAggregator {
    private class RegionCluster extends HashMap<Vector, Integer> {
        RegionCluster() {
            super();
        }
        RegionCluster(final RegionCluster regionCluster) {
            super(regionCluster);
        }
    }

    private class Area extends ArrayList<Vector> {
    }
    private class ClusterArea extends HashMap<Integer, Area> {
    }

    private final World world;
    private final Vector scale;
    private final int searchDepth;

    private RegionCluster regionCluster = new RegionCluster();
    private final ClusterArea clusterArea = new ClusterArea();

    private static final String KEY_DELIMITER = ":";

    ClusterAggregator(final World world, final Vector scale, final int searchDepth) {
        this.world = world;
        this.scale = scale;
        this.searchDepth = searchDepth;
    }

    <T extends Entity> void populate(Collection<T> entities) {
        final PopulationMap populationMap = new PopulationMap();

        for (final Entity entity : entities) {
            populationMap.add(getRegion(entity.getLocation().toVector()));
        }

        populate(populationMap);
    }

    Set<Integer> getAllClusterIds() {
        return clusterArea.keySet();
    }

    <T extends Entity> List<T> fetchEntities(Class<T> tClass, BiConsumer<Integer,T> consumer) {
        List<T> lostEntities = new ArrayList<>();
        for (final T entity : world.getEntitiesByClass(tClass)) {
            final Integer clusterId = regionCluster.get(getRegion(entity.getLocation().toVector()));
            if(clusterId != null) {
                consumer.accept(clusterId, entity);
            } else {
                lostEntities.add(entity);
            }
        }

        return lostEntities;
    }

    void fetchBlocksInCluster(int clusterId, Consumer<Block> consumer) {
        final Area area = clusterArea.get(clusterId);
        if(area == null) {
            throw new VillageInfoError(String.format("Cluster #%d not found.", clusterId));
        }

        final Set<Vector> nearRegion = new HashSet<>();
        for (final Vector region : area) {
            final int x1 = region.getBlockX() - searchDepth;
            final int x2 = region.getBlockX() + searchDepth;
            final int y1 = region.getBlockY() - searchDepth;
            final int y2 = region.getBlockY() + searchDepth;
            final int z1 = region.getBlockZ() - searchDepth;
            final int z2 = region.getBlockZ() + searchDepth;
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        nearRegion.add(new Vector(x, y, z));
                    }
                }
            }
        }

        for (final Vector region : nearRegion) {
            final int x1 = (region.getBlockX()) * scale.getBlockX();
            final int x2 = (region.getBlockX() + 1) * scale.getBlockX() - 1;
            final int y1 = (region.getBlockY()) * scale.getBlockY();
            final int y2 = (region.getBlockY() + 1) * scale.getBlockY() - 1;
            final int z1 = (region.getBlockZ()) * scale.getBlockZ();
            final int z2 = (region.getBlockZ() + 1) * scale.getBlockZ() - 1;

            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        consumer.accept(world.getBlockAt(x, y, z));
                    }
                }
            }
        }
    }

    StorageData getDump() {
        final StorageData data = new StorageData();
        final String[] keyParts = new String[3];
        for (final Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector region = entry.getKey();
            final Integer clusterId = entry.getValue();

            keyParts[0] = String.valueOf(region.getBlockX());
            keyParts[1] = String.valueOf(region.getBlockY());
            keyParts[2] = String.valueOf(region.getBlockZ());
            data.put(StringUtils.join(keyParts, KEY_DELIMITER), clusterId.toString());
        }

        return data;
    }

    void loadFromDump(StorageData data) {
        final RegionCluster newRegionCluster = new RegionCluster();

        for (final Map.Entry<String,String> entry : data.entrySet()) {
            final String key = entry.getKey();

            final String[] items = StringUtils.split(key, KEY_DELIMITER);
            if(items.length != 3) {
                throw new VillageInfoError(String.format("Can't load from dump: key '%s' is invalid", key));
            }

            final Vector region;
            try {
                region = new Vector(Integer.valueOf(items[0]),
                        Integer.valueOf(items[1]),
                        Integer.valueOf(items[2]));
            } catch (NumberFormatException e) {
                throw new VillageInfoError(String.format("Can't load from dump: %s", e.toString()));
            }

            final Integer clusterId = Integer.valueOf(entry.getValue());
            newRegionCluster.put(region, clusterId);
        }
        regionCluster = newRegionCluster;
        calculateClusterArea();
    }

    // ==== PRIVATE METHODS ====

    void populate(PopulationMap populationMap) {
        // Some regions aren't loaded, so we need to keep them in the database though there are no entities loaded there.
        // ClusterId is an auto-increment value, so we need to calculate the max existing id.
        int unloadedMaxClusterId = 0;
        final RegionCluster unloadedRegionCluster = new RegionCluster();
        for (final Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector region = entry.getKey();
            final int clusterId = entry.getValue();

            if (!isRegionLoaded(region)) {
                unloadedRegionCluster.put(region, clusterId);
                unloadedMaxClusterId = Math.max(unloadedMaxClusterId, clusterId);
            }
        }
        // In order to keep the stable ClusterId distribution, we need to keep the original ids when it's possible.
        final RegionCluster origRegionCluster = new RegionCluster(regionCluster);
        // Start from the unloadedRegionCluster.
        final RegionCluster newRegionCluster = new RegionCluster(unloadedRegionCluster);
        boolean isFixed = false;
        while (!isFixed) {
            // Start from the unloadedMaxClusterId.
            int maxClusterId = unloadedMaxClusterId;

            for (final Vector region : populationMap) {
                // Try to find the original ClusterId. Assume that origRegionCluster is always optimized and does not to be simplified.
                Integer newClusterId = origRegionCluster.get(region);

                if(newClusterId == null) {
                    // Try to find the nearest cluster.
                    newClusterId = findNearClusterId(newRegionCluster, region);

                    if (newClusterId == null) {
                        newClusterId = maxClusterId + 1;
                    }
                }

                maxClusterId = Math.max(maxClusterId, newClusterId);
                newRegionCluster.put(region, newClusterId);
            }

            if (newRegionCluster.equals(regionCluster)) {
                isFixed = true;
            }
            regionCluster = newRegionCluster;
        }

        calculateClusterArea();
    }

    private void calculateClusterArea() {
        clusterArea.clear();
        for (final Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector region = entry.getKey();
            final Integer clusterId = entry.getValue();

            Area area = clusterArea.get(region);
            if (area == null) {
                area = new Area();
                clusterArea.put(clusterId, area);
            }
            area.add(region);
        }
    }

    private Integer findNearClusterId(RegionCluster regionCluster, Vector region) {
        final int x1 = region.getBlockX() - searchDepth;
        final int x2 = region.getBlockX() + searchDepth;
        final int y1 = region.getBlockY() - searchDepth;
        final int y2 = region.getBlockY() + searchDepth;
        final int z1 = region.getBlockZ() - searchDepth;
        final int z2 = region.getBlockZ() + searchDepth;

        Integer minNearClusterId = null;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    final Vector nearRegion = new Vector(x, y, z);
                    if (!nearRegion.equals(region)) {
                        final Integer nearClusterId = regionCluster.get(nearRegion);
                        if (nearClusterId != null) {
                            if(minNearClusterId == null) {
                                minNearClusterId = nearClusterId;
                            } else {
                                minNearClusterId = Math.min(minNearClusterId, nearClusterId);
                            }
                        }
                    }
                }
            }
        }

        return minNearClusterId;
    }

    boolean isRegionLoaded(Vector region) {
        final int x = region.getBlockX() * scale.getBlockX();
        final int z = region.getBlockZ() * scale.getBlockZ();
        return isBlockLoaded(x, z)
                && isBlockLoaded(x + scale.getBlockX() - 1, z + scale.getBlockZ() - 1);
    }

    boolean isBlockLoaded(int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }

    Vector getRegion(Vector vector) {
        return new Vector(
                vector.getBlockX() / scale.getBlockX(),
                vector.getBlockY() / scale.getBlockY(),
                vector.getBlockZ() / scale.getBlockZ());
    }

    // ==== COMMON METHODS ====

    @Override
    public String toString() {
        final Map<String,String> map = new HashMap<>();
        map.put("world", world.toString());
        map.put("scale", scale.toString());
        map.put("searchDepth", String.valueOf(searchDepth));
        map.put("regionCluster", regionCluster.toString());

        return map.toString();
    }
}