package dev.xorcery.opentelemetry.exporters.websocket;

import dev.xorcery.configuration.Configuration;

import java.time.Duration;

public record WebsocketExporterMetricsConfiguration(Configuration configuration) {
    public static WebsocketExporterMetricsConfiguration get(Configuration configuration) {
        return new WebsocketExporterMetricsConfiguration(configuration.getConfiguration("opentelemetry.exporters.websocket.metrics"));
    }

    // Metrics
    public Duration getInterval() {
        return Duration.parse("PT" + configuration.getString("interval").orElse("30s"));
    }
}
