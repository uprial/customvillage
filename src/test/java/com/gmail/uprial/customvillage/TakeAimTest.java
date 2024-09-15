package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.helpers.TestConfigBase;
import org.junit.Test;

public class TakeAimTest extends TestConfigBase {
    @Test
    public void testLoadException() throws Exception {
        CustomVillage.loadConfig(getPreparedConfig(""), getCustomLogger());
    }
}