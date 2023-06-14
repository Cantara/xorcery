package com.exoreaction.xorcery.configuration.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration builder messages to be logged later by Xorcery
 */
public final class ConfigurationLogger {

    private static final ConfigurationLogger logger = new ConfigurationLogger();

    public static ConfigurationLogger getLogger() {
        return logger;
    }

    private List<String> messages = new ArrayList<>();

    public void log(String message) {
        messages.add(message);
    }

    public List<String> drain() {
        try {
            return messages;
        } finally {
            messages = new ArrayList<>();
        }
    }
}
