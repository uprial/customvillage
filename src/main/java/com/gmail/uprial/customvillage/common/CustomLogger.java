package com.gmail.uprial.customvillage.common;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomLogger {
    private boolean debugMode = true;

    private final Logger logger;
    private final CommandSender sender;

    public CustomLogger(Logger logger) {
        this(logger, null);
    }

    public CustomLogger(Logger logger, CommandSender sender) {
        this.logger = logger;
        this.sender = sender;
    }

    public void setDebugMode(boolean value) {
        debugMode = value;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void debug(String message) {
        if (debugMode) {
            //Never ever show debug messages to user
            log2console(Level.INFO, String.format("[DEBUG] %s", message));
        }
    }

    public void info(String message) {
        log(Level.INFO, "", null, message);
    }

    public void warning(String message) {
        log(Level.WARNING, "WARNING", ChatColor.YELLOW, message);
    }

    public void error(String message) {
        log(Level.SEVERE, "ERROR", ChatColor.RED, message);
    }

    private void log(Level level, String messageType, ChatColor color, String message) {
        String consoleMessage = !messageType.isEmpty()
                ? String.format("[%s] %s", messageType, message) : message;

        if ((sender != null) && (!sender.getName().equals("CONSOLE"))) {
            log2console(level, String.format("%s [user=%s]", consoleMessage, sender.getName()));

            String userMessage = message;
            if (!messageType.isEmpty()) {
                userMessage = String.format("%s: %s", messageType, userMessage);
            }
            if (color != null) {
                userMessage = color + userMessage;
            }
            sender.sendMessage(userMessage);
        } else {
            log2console(level, consoleMessage);
        }
    }

    protected void log2console(Level level, String message) {
        logger.log(level, message);
    }
}
