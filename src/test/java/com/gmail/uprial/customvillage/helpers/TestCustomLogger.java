package com.gmail.uprial.customvillage.helpers;

import com.gmail.uprial.customvillage.common.CustomLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

class TestCustomLogger extends CustomLogger {
    private boolean failOnDebug = false;
    private boolean failOnAny = false;
    private boolean failOnError = true;

    TestCustomLogger() {
        super(Logger.getLogger("test"));
    }

    public void doFailOnDebug() {
        failOnDebug = true;
    }

    public void doFailOnAny() {
        failOnAny = true;
    }

    public void doNotFailOnError() {
        failOnError = false;
    }

    @Override
    protected void log2console(Level level, String message) {
        if (failOnAny || ((failOnError) && ((level == Level.SEVERE) || (level == Level.WARNING)))) {
            fail(message);
        }
    }

    @Override
    public void debug(String message) {
        if (failOnDebug || failOnAny) {
            fail(message);
        }
    }

    private static void fail(String message) {
        throw new RuntimeException(message);
    }
}
