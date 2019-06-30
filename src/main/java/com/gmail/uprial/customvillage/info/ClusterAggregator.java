package com.gmail.uprial.customvillage.info;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;

class ClusterAggregator {
    private class Population extends ArrayList<Vector> {
    }
    private class PopulationMap extends HashMap<Vector, Population> {
    }
    private class ClusterMap extends HashMap<Vector, Integer> {
        ClusterMap() {
            super();
        }
        ClusterMap(final ClusterMap clusterMap) {
            super(clusterMap);
        }
    }

    private final World world;
    private final Vector scale;
    private final int searchDepth;

    private PopulationMap populationMap = new PopulationMap();
    private ClusterMap clusterMap = new ClusterMap();
    private boolean optimized = false;

    ClusterAggregator(final World world, final Vector scale, final int searchDepth) {
        this.world = world;
        this.scale = scale;
        this.searchDepth = searchDepth;
    }

    void add(final Vector vector) {
        final Vector normalizedVector = getNormalizedVector(vector);

        Population population = populationMap.get(normalizedVector);
        if(population == null) {
            population = new Population();
            populationMap.put(normalizedVector, population);
        }
        population.add(vector);
        flush();
    }

    Set<Integer> getAllClusterIds() {
        optimize();

        return new HashSet<>(clusterMap.values());
    }

    Integer getClusterId(Vector vector) {
        optimize();
        return clusterMap.get(getNormalizedVector(vector));
    }

    void fetchBlocksInCluster(int clusterId, Consumer<Block> consumer) {
        Set<Vector> regions = new HashSet<>();
        for (Map.Entry<Vector, Integer> entry : clusterMap.entrySet()) {
            if (entry.getValue().equals(clusterId)) {
                final Vector vector = entry.getKey();
                final int x1 = vector.getBlockX() - searchDepth;
                final int x2 = vector.getBlockX() + searchDepth;
                final int y1 = vector.getBlockY() - searchDepth;
                final int y2 = vector.getBlockY() + searchDepth;
                final int z1 = vector.getBlockZ() - searchDepth;
                final int z2 = vector.getBlockZ() + searchDepth;
                for (int x = x1; x <= x2; x++) {
                    for (int y = y1; y <= y2; y++) {
                        for (int z = z1; z <= z2; z++) {
                            regions.add(new Vector(x, y, z));
                        }
                    }
                }
            }
        }

        for (Vector vector : regions) {
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

    private void flush() {
        optimized = false;
    }

    private void optimize() {
        if(optimized) {
            return;
        }
        int unloadedClusterIdCounter = 0;
        final ClusterMap unloadedClusterMap = new ClusterMap();
        for (final Map.Entry<Vector, Integer> entry : clusterMap.entrySet()) {
            final Vector vector = entry.getKey();
            final int clusterId = entry.getValue();

            if(!isRegionLoaded(vector)) {
                unloadedClusterMap.put(vector, clusterId);
                unloadedClusterIdCounter = Math.max(unloadedClusterIdCounter, clusterId);
            }
        }

        boolean isFixed = false;
        while(!isFixed) {
            int clusterIdCounter = unloadedClusterIdCounter;
            final ClusterMap newClusterMap = new ClusterMap(unloadedClusterMap);

            for (final Map.Entry<Vector, Population> entry : populationMap.entrySet()) {
                final Population population = entry.getValue();

                if(!population.isEmpty()) {
                    final Vector vector = entry.getKey();

                    Integer newClusterId = findNearClusterId(newClusterMap, vector);
                    if (newClusterId == null) {
                        clusterIdCounter++;
                        newClusterId = clusterIdCounter;
                    }

                    newClusterMap.put(vector, newClusterId);
                }
            }

            if(newClusterMap.equals(clusterMap)) {
                isFixed = true;
            }
            clusterMap = newClusterMap;
        }
        optimized = true;
    }

    private Integer findNearClusterId(ClusterMap clusterMap, Vector vector) {
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
                        final Integer nearClusterId = clusterMap.get(nearVector);
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
        return world.isChunkLoaded(x, z)
                && world.isChunkLoaded(x + scale.getBlockX(), z + scale.getBlockZ());
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
        map.put("clusterMap", clusterMap.toString());

        return map.toString();
    }
}