package com.exoreaction.xorcery.configuration.model;

import com.exoreaction.xorcery.builders.WithContext;

import java.time.Duration;

/**
 * Configuration wrapper for standard defaults.
 */
public record DefaultsConfiguration(Configuration configuration) {
    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + configuration.getString("idleTimeout").orElse("-1s"));
    }
}
