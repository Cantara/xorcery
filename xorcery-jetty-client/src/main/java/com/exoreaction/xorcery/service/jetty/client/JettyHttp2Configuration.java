package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.time.Duration;

public record JettyHttp2Configuration(Configuration configuration) {
    public boolean isEnabled() {
        return configuration.getBoolean("enabled").orElse(false);
    }

    public long getIdleTimeout() {
        return Duration.parse("PT" + configuration.getString("idleTimeout").orElse("-1s")).toSeconds();
    }
}
