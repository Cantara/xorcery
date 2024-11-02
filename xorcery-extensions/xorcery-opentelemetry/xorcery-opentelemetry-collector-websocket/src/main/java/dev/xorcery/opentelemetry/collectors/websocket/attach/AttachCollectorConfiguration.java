package dev.xorcery.opentelemetry.collectors.websocket.attach;

import dev.xorcery.configuration.Configuration;

import java.time.Duration;

public record AttachCollectorConfiguration(Configuration configuration) {
    public static AttachCollectorConfiguration get(Configuration configuration) {
        return new AttachCollectorConfiguration(configuration.getConfiguration("opentelemetry.collectors.listen"));
    }

    public Duration getMinimumBackoff() {
        return Duration.parse("PT" + configuration.getString("backoff").orElse("30s"));
    }
}
