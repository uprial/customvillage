package com.gmail.uprial.customvillage.info;

import org.bukkit.util.Vector;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ClusterAggregatorTest {
    private static final Vector TEST_CLUSTER_SCALE = new Vector(32, 5, 32);
    private static final int TEST_CLUSTER_SEARCH_DEPTH = 1;

    private class TestClusterAggregator extends ClusterAggregator {
        //
        private int loadedSquareCenterX = 200;
        private int loadedSquareCenterZ = 200;

        TestClusterAggregator() {
            super(null, TEST_CLUSTER_SCALE, TEST_CLUSTER_SEARCH_DEPTH);
        }

        @Override
        boolean isBlockLoaded(int x, int z) {
            return (Math.abs(x - loadedSquareCenterX) <= 100) && (Math.abs(z - loadedSquareCenterZ) <= 100);
        }

        void moveLoadedSquareCenter(int x, int z) {
            loadedSquareCenterX = x;
            loadedSquareCenterZ = z;
        }
    }

    private ClusterAggregator aggregator = null;

    @Before
    public void setUp() throws Exception {
        aggregator = new TestClusterAggregator();
    }

    @Test
    public void testGetRegion() throws Exception {
        List<Vector> list = new ArrayList<Vector>() {{
            add(new Vector(1, 1, 1));
            add(new Vector(0, 0, 0));

            add(new Vector(31, 1, 1));
            add(new Vector(0, 0, 0));

            add(new Vector(32, 1, 1));
            add(new Vector(1, 0, 0));

            add(new Vector(1, 4, 1));
            add(new Vector(0, 0, 0));

            add(new Vector(1, 5, 1));
            add(new Vector(0, 1, 0));

            add(new Vector(1, 1, 31));
            add(new Vector(0, 0, 0));

            add(new Vector(1, 1, 32));
            add(new Vector(0, 0, 1));
        }};

        for (int i = 0; i < list.size() / 2; i++) {
            assertEquals(list.get(i * 2 + 1).toString(),
                    aggregator.getRegion(list.get(i * 2)).toString());
        }
    }

    @Test
    public void testIsRegionLoaded() throws Exception {
        // 4 * 32 = 128, 128 > 100 -> isBlockLoaded
        assertFalse(aggregator.isRegionLoaded(new Vector(1, 1, 1)));
        assertFalse(aggregator.isRegionLoaded(new Vector(2, 1, 1)));
        assertFalse(aggregator.isRegionLoaded(new Vector(4, 1, 1)));
        assertFalse(aggregator.isRegionLoaded(new Vector(4, 1, 2)));
        assertTrue(aggregator.isRegionLoaded(new Vector(4, 1, 4)));
    }

    @Test
    public void testEmptyPopulation() throws Exception {
        aggregator.populate(new PopulationMap());
        assertEquals("{}", aggregator.getDump().toString());
    }

    @Test
    public void testSinglePopulation() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
        }});
        assertEquals("{1:1:1=1}", aggregator.getDump().toString());
    }

    @Test
    public void testSingleCluster() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(2, 2, 2));
        }});
        assertEquals("{1:1:1=1, 2:2:2=1}", aggregator.getDump().toString());
    }

    @Test
    public void testTwoGroupIterations() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(4, 1, 1));
            add(new Vector(2, 1, 1));
            add(new Vector(3, 1, 1));
        }});
        assertEquals("{1:1:1=1, 4:1:1=1, 3:1:1=1, 2:1:1=1}", aggregator.getDump().toString());
    }

    @Test
    public void testThreeGroupIterations() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(6, 1, 1));
            add(new Vector(2, 1, 1));
            add(new Vector(5, 1, 1));
            add(new Vector(3, 1, 1));
            add(new Vector(4, 1, 1));
        }});
        assertEquals("{6:1:1=1, 1:1:1=1, 4:1:1=1, 5:1:1=1, 3:1:1=1, 2:1:1=1}", aggregator.getDump().toString());
    }

    @Test
    public void testSaveEmptyButUnloadedRegions() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
        }});
        aggregator.populate(new PopulationMap() {{
            add(new Vector(4, 4, 4));
        }});
        assertEquals("{1:1:1=1, 4:4:4=2}", aggregator.getDump().toString());
    }

    @Test
    public void testClearEmptyAndLoadedRegions() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(6, 6, 6));
        }});
        aggregator.populate(new PopulationMap() {{
            add(new Vector(4, 4, 4));
        }});
        assertEquals("{4:4:4=1}", aggregator.getDump().toString());
    }

    @Test
    public void testStableClusterId() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(3, 3, 3));
            add(new Vector(6, 6, 6));
        }});
        assertEquals("{6:6:6=2, 3:3:3=1}", aggregator.getDump().toString());

        aggregator.populate(new PopulationMap() {{
            add(new Vector(6, 6, 6));
            add(new Vector(1, 1, 1));
            add(new Vector(3, 3, 3));
        }});
        assertEquals("{1:1:1=3, 6:6:6=2, 3:3:3=1}", aggregator.getDump().toString());
    }

    @Test
    public void testStableClusterIdClearance() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(3, 3, 3));
            add(new Vector(6, 6, 6));
        }});
        assertEquals("{6:6:6=2, 3:3:3=1}", aggregator.getDump().toString());

        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(3, 3, 3));
        }});
        assertEquals("{1:1:1=2, 3:3:3=1}", aggregator.getDump().toString());
    }

    @Test
    public void testIntersectionOfUnloadedInSeveralIterations() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(3, 3, 3));
            add(new Vector(6, 6, 6));
        }});
        assertEquals("{1:1:1=3, 6:6:6=2, 3:3:3=1}", aggregator.getDump().toString());

        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, -1));
            add(new Vector(1, 1, 0));
            add(new Vector(1, 1, 2));
            add(new Vector(1, 1, 3));
        }});
        assertEquals("{1:1:1=3, 1:1:-1=3, 1:1:0=3, 3:3:3=1, 1:1:3=3, 1:1:2=3}", aggregator.getDump().toString());
    }

    @Test
    public void testStableMergeWithLowerId() throws Exception {
        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(4, 4, 4));
            add(new Vector(6, 6, 6));
        }});
        assertEquals("{1:1:1=2, 6:6:6=1, 4:4:4=3}", aggregator.getDump().toString());

        aggregator.populate(new PopulationMap() {{
            add(new Vector(1, 1, 1));
            add(new Vector(4, 4, 4));
        }});
        assertEquals("{1:1:1=2, 4:4:4=3}", aggregator.getDump().toString());
    }
}