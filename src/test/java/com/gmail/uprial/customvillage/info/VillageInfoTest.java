package com.gmail.uprial.customvillage.info;

import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.storage.CustomStorage;
import com.gmail.uprial.customvillage.storage.StorageData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Villager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class VillageInfoTest {
    private static class TestVillageInfo extends VillageInfo {
        TestVillageInfo(final CustomLogger customLogger) {
            super(null, customLogger);

        }

        @Override
        public void update() {

        }

        @Override
        CustomStorage getStorage(final World world) {
            final CustomStorage customStorage = mock(CustomStorage.class);

            final StorageData storageData = new StorageData();
            storageData.put("-10:6:62", "14");
            storageData.put("-7:3:39", "20");
            storageData.put("-7:5:41", "17");
            storageData.put("-7:5:42", "17");
            storageData.put("-6:4:42", "17");
            storageData.put("-6:5:41", "17");
            storageData.put("-6:5:42", "17");

            when(customStorage.load()).thenReturn(storageData);

            return customStorage;
        }
    }

    private VillageInfo villageInfo = null;
    private World world = null;

    @Before
    public void setUp() throws Exception {
        final CustomLogger customLogger = mock(CustomLogger.class);
        when(customLogger.isDebugMode()).thenReturn(false);

        villageInfo = new TestVillageInfo(customLogger);

        world = mock(World.class);

        final List<Location> locations = new ArrayList<>();
        locations.add(new Location(world, -297, 75, 1991));
        locations.add(new Location(world, -191, 62, 1342));
        locations.add(new Location(world, -182, 62, 1342));
        locations.add(new Location(world, -179, 62, 1342));
        locations.add(new Location(world, -184, 62, 1341));
        locations.add(new Location(world, -178, 62, 1342));
        locations.add(new Location(world, -188, 62, 1349));
        locations.add(new Location(world, -188, 54, 1346));
        locations.add(new Location(world, -191, 62, 1348));
        locations.add(new Location(world, -140, 62, 1346));
        locations.add(new Location(world, -140, 62, 1346));
        locations.add(new Location(world, -164, 62, 1342));
        locations.add(new Location(world, -168, 62, 1342));
        locations.add(new Location(world, -175, 62, 1342));
        locations.add(new Location(world, -195, 62, 1342));
        locations.add(new Location(world, -142, 62, 1346));
        locations.add(new Location(world, -193, 62, 1349));
        locations.add(new Location(world, -195, 62, 1349));
        locations.add(new Location(world, -208, 47, 1270));
        locations.add(new Location(world, -208, 47, 1269));
        locations.add(new Location(world, -208, 47, 1269));

        final Collection<Villager> villagers = new ArrayList<>();
        for(final Location location : locations) {
            final Villager villager = mock(Villager.class);
            when(villager.getLocation()).thenReturn(location);
            when(villager.getUniqueId()).thenReturn(UUID.randomUUID());

            villagers.add(villager);
        }

        when(world.getEntitiesByClass(Villager.class)).thenReturn(villagers);
    }

    @After
    public void tearDown() {
        villageInfo = null;
        world = null;
    }

    @Test
    public void testUnloadedChunks() throws Exception {
        assertEquals("{-1=Village{villagers: 0, bed-heads: 0, " +
                "iron-golems{natural: 0, user: 0, all: 0}, " +
                "cats{natural: 0, user: 0, all: 0}}}",
                villageInfo.getVillages(world).toString());
    }

    @Test
    public void testLoadedChunks() throws Exception {
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);

        final Block block = mock(Block.class);
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);

        assertEquals("{-1=Village{villagers: 0, bed-heads: 0, " +
                        "iron-golems{natural: 0, user: 0, all: 0}, " +
                        "cats{natural: 0, user: 0, all: 0}}, " +
                        "1=Village{villagers: 17, bed-heads: 0, " +
                        "iron-golems{natural: 0, user: 0, all: 0}, " +
                        "cats{natural: 0, user: 0, all: 0}}, " +
                        "20=Village{villagers: 3, bed-heads: 0, " +
                        "iron-golems{natural: 0, user: 0, all: 0}, " +
                        "cats{natural: 0, user: 0, all: 0}}, " +
                        "14=Village{villagers: 1, bed-heads: 0, " +
                        "iron-golems{natural: 0, user: 0, all: 0}, " +
                        "cats{natural: 0, user: 0, all: 0}}}",
                villageInfo.getVillages(world).toString());
    }
}