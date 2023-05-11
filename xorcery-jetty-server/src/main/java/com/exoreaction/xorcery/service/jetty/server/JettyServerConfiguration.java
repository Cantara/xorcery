package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;

public record JettyServerConfiguration(Configuration configuration) {

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + configuration.getString("idleTimeout").orElse("-1s"));
    }

    public boolean isHttpEnabled() {
        return configuration.getBoolean("http.enabled").orElse(true);
    }

    public int getHttpPort() {
        return configuration.getInteger("http.port").orElse(8889);
    }

    public int getMinThreads() {
        return configuration.getInteger("minThreads").orElse(10);
    }

    public int getMaxThreads() {
        return configuration.getInteger("maxThreads").orElse(150);
    }

    public int getOutputBufferSize() {
        return configuration.getInteger("outputBufferSize").orElse(32768);
    }

    public int getRequestHeaderSize() {
        return configuration.getInteger("requestHeaderSize").orElse(16384);
    }
}
