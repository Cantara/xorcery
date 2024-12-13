package dev.xorcery.opentelemetry.exporters.reactivestreams;

import dev.xorcery.configuration.Configuration;

import java.time.Duration;

public record ReactiveStreamExporterMetricsConfiguration(Configuration configuration) {
    public static ReactiveStreamExporterMetricsConfiguration get(Configuration configuration) {
        return new ReactiveStreamExporterMetricsConfiguration(configuration.getConfiguration("opentelemetry.exporters.websocket.metrics"));
    }

    // Metrics
    public Duration getInterval() {
        return Duration.parse("PT" + configuration.getString("interval").orElse("30s"));
    }
}
