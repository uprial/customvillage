package com.gmail.uprial.customvillage.info;

import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

class ClusterAggregator {
    private class Population extends ArrayList<Vector> {
    }
    private class PopulationMap extends HashMap<Vector, Population> {
    }

    private class RegionCluster extends HashMap<Vector, Integer> {
        RegionCluster() {
            super();
        }
        RegionCluster(final RegionCluster regionCluster) {
            super(regionCluster);
        }
    }

    private class Region extends ArrayList<Vector> {
    }
    private class ClusterRegion extends HashMap<Integer, Region> {
    }

    private final World world;
    private final Vector scale;
    private final int searchDepth;

    private RegionCluster regionCluster = new RegionCluster();
    private final ClusterRegion clusterRegion = new ClusterRegion();

    private static final String KEY_DELIMITER = ":";

    ClusterAggregator(final World world, final Vector scale, final int searchDepth) {
        this.world = world;
        this.scale = scale;
        this.searchDepth = searchDepth;
    }

    <T extends Entity> void populate(Collection<T> entities) {
        final PopulationMap populationMap = new PopulationMap();

        for (Entity entity : entities) {
            final Vector vector = entity.getLocation().toVector();
            final Vector normalizedVector = getNormalizedVector(vector);

            Population population = populationMap.get(normalizedVector);
            if (population == null) {
                population = new Population();
                populationMap.put(normalizedVector, population);
            }
            population.add(vector);
        }

        optimize(populationMap);
    }

    Set<Integer> getAllClusterIds() {
        return clusterRegion.keySet();
    }

    <T extends Entity> List<T> fetchEntities(Class<T> tClass, BiConsumer<Integer,T> consumer) {
        List<T> lostEntities = new ArrayList<>();
        for (T entity : world.getEntitiesByClass(tClass)) {
            final Integer clusterId = regionCluster.get(getNormalizedVector(entity.getLocation().toVector()));
            if(clusterId != null) {
                consumer.accept(clusterId, entity);
            } else {
                lostEntities.add(entity);
            }
        }

        return lostEntities;
    }

    void fetchBlocksInCluster(int clusterId, Consumer<Block> consumer) {
        final Region region = clusterRegion.get(clusterId);
        if(region == null) {
            throw new VillageInfoError(String.format("Cluster #%d not found.", clusterId));
        }

        final Set<Vector> nearRegion = new HashSet<>();
        for (Vector vector : region) {
            final int x1 = vector.getBlockX() - searchDepth;
            final int x2 = vector.getBlockX() + searchDepth;
            final int y1 = vector.getBlockY() - searchDepth;
            final int y2 = vector.getBlockY() + searchDepth;
            final int z1 = vector.getBlockZ() - searchDepth;
            final int z2 = vector.getBlockZ() + searchDepth;
            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        nearRegion.add(new Vector(x, y, z));
                    }
                }
            }
        }

        for (Vector vector : nearRegion) {
            final int x1 = (vector.getBlockX()) * scale.getBlockX();
            final int x2 = (vector.getBlockX() + 1) * scale.getBlockX() - 1;
            final int y1 = (vector.getBlockY()) * scale.getBlockY();
            final int y2 = (vector.getBlockY() + 1) * scale.getBlockY() - 1;
            final int z1 = (vector.getBlockZ()) * scale.getBlockZ();
            final int z2 = (vector.getBlockZ() + 1) * scale.getBlockZ() - 1;

            for (int x = x1; x <= x2; x++) {
                for (int y = y1; y <= y2; y++) {
                    for (int z = z1; z <= z2; z++) {
                        consumer.accept(world.getBlockAt(x, y, z));
                    }
                }
            }
        }
    }

    Map<String, String> getDump() {
        final Map<String, String> map = new HashMap<>();
        final String[] keyParts = new String[3];
        for (Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector vector = entry.getKey();
            final Integer clusterId = entry.getValue();

            keyParts[0] = String.valueOf(vector.getBlockX());
            keyParts[1] = String.valueOf(vector.getBlockY());
            keyParts[2] = String.valueOf(vector.getBlockZ());
            map.put(StringUtils.join(keyParts, KEY_DELIMITER), clusterId.toString());
        }

        return map;
    }

    void loadFromDump(Map<String, String> map) {
        final RegionCluster newRegionCluster = new RegionCluster();

        for (Map.Entry<String,String> entry : map.entrySet()) {
            final String key = entry.getKey();

            final String[] items = StringUtils.split(key, KEY_DELIMITER);
            if(items.length != 3) {
                throw new VillageInfoError(String.format("Can't load from dump: key '%s' is invalid", key));
            }

            final Vector vector;
            try {
                vector = new Vector(Integer.valueOf(items[0]),
                        Integer.valueOf(items[1]),
                        Integer.valueOf(items[2]));
            } catch (NumberFormatException e) {
                throw new VillageInfoError(String.format("Can't load from dump: %s", e.toString()));
            }

            final Integer clusterId = Integer.valueOf(entry.getValue());
            newRegionCluster.put(vector, clusterId);
        }
        regionCluster = newRegionCluster;
        calculateClusterRegion();
    }

    // ==== PRIVATE METHODS ====

    private void optimize(PopulationMap populationMap) {
        // ClusterId is an auto-increment value, so we need to calculate the max existing id.
        int maxInitialClusterId = 0;
        // Some regions aren't loaded, so we need to keep them in the database though there are no entities loaded there.
        final RegionCluster unloadedRegionCluster = new RegionCluster();
        for (final Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector vector = entry.getKey();
            final int clusterId = entry.getValue();

            if (!isRegionLoaded(vector)) {
                unloadedRegionCluster.put(vector, clusterId);
            }
            maxInitialClusterId = Math.max(maxInitialClusterId, clusterId);
        }
        // In order to keep the stable ClusterId distribution, we need to keep the original ids when it's possible.
        final RegionCluster origRegionCluster = new RegionCluster(regionCluster);

        boolean isFixed = false;
        while (!isFixed) {
            int clusterIdCounter = maxInitialClusterId + 1;
            // Start from the unloaded regions.
            final RegionCluster newRegionCluster = new RegionCluster(unloadedRegionCluster);

            for (final Map.Entry<Vector, Population> entry : populationMap.entrySet()) {
                final Population population = entry.getValue();

                // If there are some entities...
                if (!population.isEmpty()) {
                    final Vector vector = entry.getKey();

                    // Try to find the original ClusterId. Assume that origRegionCluster is always optimized and does not to be simplified.
                    Integer newClusterId = origRegionCluster.get(vector);

                    if(newClusterId == null) {
                        // Try to find the nearest cluster.
                        newClusterId = findNearClusterId(newRegionCluster, vector);

                        if (newClusterId == null) {
                            newClusterId = clusterIdCounter;
                            clusterIdCounter++;
                        }
                    }

                    newRegionCluster.put(vector, newClusterId);
                }
            }

            if (newRegionCluster.equals(regionCluster)) {
                isFixed = true;
            }
            regionCluster = newRegionCluster;
        }

        calculateClusterRegion();
    }

    private void calculateClusterRegion() {
        clusterRegion.clear();
        for (Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector vector = entry.getKey();
            final Integer clusterId = entry.getValue();

            Region region = clusterRegion.get(vector);
            if (region == null) {
                region = new Region();
                clusterRegion.put(clusterId, region);
            }
            region.add(vector);
        }
    }

    private Integer findNearClusterId(RegionCluster regionCluster, Vector vector) {
        final int x1 = vector.getBlockX() - searchDepth;
        final int x2 = vector.getBlockX() + searchDepth;
        final int y1 = vector.getBlockY() - searchDepth;
        final int y2 = vector.getBlockY() + searchDepth;
        final int z1 = vector.getBlockZ() - searchDepth;
        final int z2 = vector.getBlockZ() + searchDepth;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    final Vector nearVector = new Vector(x, y, z);
                    if (!nearVector.equals(vector)) {
                        final Integer nearClusterId = regionCluster.get(nearVector);
                        if (nearClusterId != null) {
                            return nearClusterId;
                        }
                    }
                }
            }
        }

        return null;
    }

    private boolean isRegionLoaded(Vector vector) {
        final int x = vector.getBlockX() * scale.getBlockX();
        final int z = vector.getBlockZ() * scale.getBlockZ();
        return isBlockLoaded(x, z)
                && isBlockLoaded(x + scale.getBlockX() - 1, z + scale.getBlockZ() - 1);
    }

    private boolean isBlockLoaded(int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }

    private Vector getNormalizedVector(Vector vector) {
        return new Vector(
                vector.getBlockX() / scale.getBlockX(),
                vector.getBlockY() / scale.getBlockY(),
                vector.getBlockZ() / scale.getBlockZ());
    }


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