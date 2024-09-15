package com.gmail.uprial.customvillage.common;

import com.google.common.collect.Lists;
import org.junit.Test;

import static com.gmail.uprial.customvillage.common.Utils.joinStrings;
import static org.junit.Assert.assertEquals;

public class UtilsTest {
    @Test
    public void testJoinEmptyStrings() throws Exception {
        assertEquals("", joinStrings(",", Lists.newArrayList(new String[]{})));
    }

    @Test
    public void testJoinOneString() throws Exception {
        assertEquals("a", joinStrings(",", Lists.newArrayList("a")));
    }

    @Test
    public void testJoinSeveralStrings() throws Exception {
        assertEquals("a,b", joinStrings(",", Lists.newArrayList("a", "b")));
    }
}