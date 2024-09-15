package com.gmail.uprial.customvillage.config;

import com.gmail.uprial.customvillage.helpers.TestConfigBase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.gmail.uprial.customvillage.config.ConfigReaderSimple.*;
import static org.junit.Assert.*;

@SuppressWarnings("ClassWithTooManyMethods")
public class ConfigReaderSimpleTest extends TestConfigBase {
    @Rule
    public final ExpectedException e = ExpectedException.none();

    // ==== getBoolean ====
    @Test
    public void testEmptyBoolean() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty 'value' flag. Use default value false");
        getBoolean(getPreparedConfig(""), getDebugFearingCustomLogger(), "f", "'value' flag", false);
    }

    @Test
    public void testEmptyBooleanDefaultValue() throws Exception {
        e.expect(RuntimeException.class);
        e.expectMessage("Empty 'value' flag. Use default value true");
        getBoolean(getPreparedConfig(""), getDebugFearingCustomLogger(), "f", "'value' flag", true);
    }

    @Test
    public void testInvalidBoolean() throws Exception {
        e.expect(InvalidConfigException.class);
        e.expectMessage("Invalid 'value' flag");
        getBoolean(getPreparedConfig("f: x"), getParanoiacCustomLogger(), "f", "'value' flag", false);
    }

    @Test
    public void testBooleanTrue() throws Exception {
        assertTrue(getBoolean(getPreparedConfig("f: true"), getParanoiacCustomLogger(), "f", "'value' flag", true));
    }

    @Test
    public void testBooleanTrueDifferentCase() throws Exception {
        assertTrue(getBoolean(getPreparedConfig("f: True"), getParanoiacCustomLogger(), "f", "'value' flag", true));
    }

    @Test
    public void testBooleanFalseDifferentCase() throws Exception {
        assertFalse(getBoolean(getPreparedConfig("f: False"), getParanoiacCustomLogger(), "f", "'value' flag", true));
    }
}