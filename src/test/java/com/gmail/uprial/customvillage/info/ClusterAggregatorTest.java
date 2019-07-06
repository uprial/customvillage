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

        TestClusterAggregator() {
            super(null, TEST_CLUSTER_SCALE, TEST_CLUSTER_SEARCH_DEPTH);
        }

        @Override
        boolean isBlockLoaded(int x, int z) {
            return (x > 100) && (z > 100);
        }
    }

    private ClusterAggregator aggregator = null;

    @Before
    public void setUp() throws Exception {
        aggregator = new TestClusterAggregator();
    }

    @Test
    public void testGetNormalizedVector() throws Exception {
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
                    aggregator.getNormalizedVector(list.get(i * 2)).toString());
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
}