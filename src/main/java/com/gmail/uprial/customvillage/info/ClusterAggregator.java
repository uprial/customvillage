package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.storage.StorageData;
import org.apache.commons.lang.StringUtils;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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
        final Area area = getArea(clusterId);

        final Set<Vector> nearRegions = new HashSet<>();
        for (final Vector region : area) {
            fetchNearRegions(region, nearRegions::add);
        }

        for (final Vector region : nearRegions) {
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
        Comparator<Vector> comparator = (Vector v1, Vector v2) -> {
            if(v1.getBlockX() < v2.getBlockX()) {
                return -1;
            } else if(v1.getBlockX() > v2.getBlockX()) {
                return 1;
            } else if(v1.getBlockY() < v2.getBlockY()) {
                return -1;
            } else if(v1.getBlockY() > v2.getBlockY()) {
                return 1;
            } else return Integer.compare(v1.getBlockZ(), v2.getBlockZ());
        };
        final Set<Vector> regions = new TreeSet<Vector>(comparator) {{ addAll(regionCluster.keySet()); }};

        final StorageData data = new StorageData();
        final String[] keyParts = new String[3];

        for (final Vector region: regions) {
            final Integer clusterId = regionCluster.get(region);

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

    boolean isFullyLoaded(int clusterId) {
        final Area area = getArea(clusterId);
        for(Vector region : area) {
            if(!isRegionLoaded(region)) {
                return false;
            }
        }

        return true;
    }

    // ==== PRIVATE METHODS ====

    private Area getArea(int clusterId) {
        final Area area = clusterArea.get(clusterId);
        if (area == null) {
            throw new VillageInfoError(String.format("Cluster #%d not found.", clusterId));
        }

        return area;
    }

    void populate(PopulationMap populationMap) {
        // Some regions aren't loaded, so we need to keep them in the database though there are no entities loaded there.
        // ClusterId is an auto-increment value, so we need to calculate the max existing id.
        int unloadedMaxClusterId = 0;
        final RegionCluster newRegionCluster = new RegionCluster();
        for (final Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector region = entry.getKey();
            final int clusterId = entry.getValue();

            if (!isRegionLoaded(region)) {
                newRegionCluster.put(region, clusterId);
                unloadedMaxClusterId = Math.max(unloadedMaxClusterId, clusterId);
            }
        }

        // In order to keep the stable ClusterId distribution, we need to keep the original ids when it's possible.
        final RegionCluster origRegionCluster = new RegionCluster(regionCluster);
        boolean isFixed = false;
        while (!isFixed) {
            // Start from the unloadedMaxClusterId.
            int maxClusterId = unloadedMaxClusterId;
            final Set<Integer> newClusterIds = new HashSet<>();

            for (final Vector region : populationMap) {
                Integer newClusterId;
                // Try to find the nearest cluster.
                newClusterId = findNearClusterId(newRegionCluster, region);

                if (newClusterId == null) {
                    // Try to find the original ClusterId.
                    newClusterId = origRegionCluster.get(region);

                    if ((newClusterId == null) || (newClusterIds.contains(newClusterId))) {
                        newClusterId = maxClusterId + 1;
                    }
                }

                newRegionCluster.put(region, newClusterId);
                maxClusterId = Math.max(maxClusterId, newClusterId);
                newClusterIds.add(newClusterId);
            }

            if (newRegionCluster.equals(regionCluster)) {
                isFixed = true;
            }
            // Copy newRegionCluster to change it safely.
            regionCluster = new RegionCluster(newRegionCluster);
        }

        calculateClusterArea();
    }

    private void calculateClusterArea() {
        clusterArea.clear();
        for (final Map.Entry<Vector, Integer> entry : regionCluster.entrySet()) {
            final Vector region = entry.getKey();
            final Integer clusterId = entry.getValue();

            Area area = clusterArea.get(clusterId);
            if (area == null) {
                area = new Area();
                clusterArea.put(clusterId, area);
            }
            area.add(region);
        }
    }

    private Integer findNearClusterId(RegionCluster regionCluster, Vector region) {
        AtomicInteger minNearClusterId = new AtomicInteger(-1);
        fetchNearRegions(region, (nearRegion) -> {
            if (!nearRegion.equals(region)) {
                final Integer nearClusterId = regionCluster.get(nearRegion);
                if (nearClusterId != null) {
                    if(minNearClusterId.get() == -1) {
                        minNearClusterId.set(nearClusterId);
                    } else {
                        minNearClusterId.set(Math.min(minNearClusterId.get(), nearClusterId));
                    }
                }
            }
        });

        if (minNearClusterId.get() == -1) {
            return null;
        } else {
            return minNearClusterId.get();
        }
    }

    private void fetchNearRegions(Vector region, Consumer<Vector> consumer) {
        final int x1 = region.getBlockX() - searchDepth;
        final int x2 = region.getBlockX() + searchDepth;
        final int y1 = region.getBlockY() - searchDepth;
        final int y2 = region.getBlockY() + searchDepth;
        final int z1 = region.getBlockZ() - searchDepth;
        final int z2 = region.getBlockZ() + searchDepth;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    consumer.accept(new Vector(x, y, z));
                }
            }
        }
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
        map.put("world", String.valueOf(world));
        map.put("scale", scale.toString());
        map.put("searchDepth", String.valueOf(searchDepth));
        map.put("dump", getDump().toString());

        return map.toString();
    }
}