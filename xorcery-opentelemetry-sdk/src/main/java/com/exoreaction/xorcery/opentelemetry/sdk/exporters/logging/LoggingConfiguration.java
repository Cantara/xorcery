package com.exoreaction.xorcery.opentelemetry.sdk.exporters.logging;

import com.exoreaction.xorcery.configuration.Configuration;

import java.time.Duration;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */

public record LoggingConfiguration(Configuration configuration) {

    public static LoggingConfiguration get(Configuration configuration) {
        return new LoggingConfiguration(configuration.getConfiguration("opentelemetry.exporters.logging"));
    }

    // Metrics
    public Duration getInterval() {
        return Duration.parse("PT" + configuration.getString("interval").orElse("30s"));
    }

    // Logs
    public Duration getScheduleDelay() {
        return Duration.parse("PT" + configuration.getString("scheduleDelay").orElse("5s"));
    }

    public Duration getExporterTimeout() {
        return Duration.parse("PT" + configuration.getString("exporterTimeout").orElse("1h"));
    }

    public int getMaxExportBatchSize() {
        return configuration.getInteger("maxExportBatchSize").orElse(1000);
    }

    public int getMaxQueueSize() {
        return configuration.getInteger("maxQueueSize").orElse(10000);
    }

    public String getLogsEndpoint() {
        return configuration.getString("logsEndpoint").orElse("http://localhost:4318/v1/logs");
    }

    public String getTracesEndpoint() {
        return configuration.getString("tracesEndpoint").orElse("http://localhost:4318/v1/traces");
    }

    public String getMetricsEndpoint() {
        return configuration.getString("metricsEndpoint").orElse("http://localhost:4318/v1/metrics");
    }
}
