package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.helpers.TestConfigBase;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;

public class DefaultConfigTest extends TestConfigBase {
    private static final String CONFIG_RESOURCE = "config.yml";
    private static final String BASE_DIR = Paths.get("").toAbsolutePath().toString();
    private static final String CONFIG_FILE = Paths.get(BASE_DIR, "src", "main", "resources", CONFIG_RESOURCE).toString();

    @Test
    public void testDefaultConfig() throws Exception {
        String contents = Files.toString(new File(CONFIG_FILE), Charsets.UTF_8);
        assertNotNull(loadConfig(getCustomLogger(), contents));
    }
}