package com.gmail.uprial.customvillage.info;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlainMapViewerTest {
    private static final int TEST_PLAIN_MAP_SCALE = 8;

    private PlainMapViewer viewer = null;

    @Before
    public void setUp() throws Exception {
        viewer = new PlainMapViewer(TEST_PLAIN_MAP_SCALE);
    }

    @Test
    public void testEmpty() throws Exception {
        assertEquals("", getText());
    }

    @Test
    public void testOneItem() throws Exception {
        viewer.add(10, 10);

        assertEquals("    \n" +
                "   8\n" +
                "    \n" +
                " 8 1", getText());
    }

    @Test
    public void testNegative() throws Exception {
        viewer.add(-10, -10);

        assertEquals("   -\n" +
                "   8\n" +
                "    \n" +
                "-8 1", getText());
    }

    @Test
    public void testBigNumbers() throws Exception {
        viewer.add(-12_000, 24_000);

        assertEquals("       -\n" +
                "       1\n" +
                "       2\n" +
                "       0\n" +
                "       0\n" +
                "       0\n" +
                "        \n" +
                " 24000 1", getText());
    }

    @Test
    public void testBigMap() throws Exception {
        viewer.add(-10, -10);
        viewer.add(40, 40);
        viewer.add(40, 30);
        viewer.add(50, 50);
        for (int i = 0; i < 15; i++) {
            viewer.add(10, 10);
        }

        assertEquals("    -       \n" +
                "    00012344\n" +
                "    80864208\n" +
                "            \n" +
                " -8 1       \n" +
                "  0         \n" +
                "  8   f     \n" +
                " 16         \n" +
                " 24       1 \n" +
                " 32         \n" +
                " 40       1 \n" +
                " 48        1", getText());
    }

    @Test
    public void testBigMapBelowZero() throws Exception {
        viewer.add(-112, 8);
        viewer.add(-96, 0);
        for (int i = 0; i < 15; i++) {
            viewer.add(-32, 16);
        }

        assertEquals("    -----------\n" +
                "    11000000000\n" +
                "    10988765443\n" +
                "    24680246802\n" +
                "               \n" +
                "  0   1        \n" +
                "  8 1          \n" +
                " 16           f", getText());
    }

    @Test
    public void testMapNearZero() throws Exception {
        viewer.add(7, 0);

        assertEquals("    \n" +
                "   0\n" +
                "    \n" +
                " 0 1", getText());
    }

    @Test
    public void testToString() throws Exception {
        viewer.add(-10, -10);
        viewer.add(20, 20);

        assertEquals("{scale=8, map={(2, 2)=1, (-1, -1)=1}}", viewer.toString());
    }

    // ==== PRIVATE METHODS ====

    private String getText() {
        return StringUtils.join(viewer.getTextLines(), "\n");
    }
}