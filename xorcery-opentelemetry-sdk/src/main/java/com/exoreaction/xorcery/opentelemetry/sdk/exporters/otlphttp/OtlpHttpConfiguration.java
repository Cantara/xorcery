package com.exoreaction.xorcery.opentelemetry.sdk.exporters.otlphttp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.secrets.Secrets;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public record OtlpHttpConfiguration(Configuration configuration) {

    public static OtlpHttpConfiguration get(Configuration configuration)
    {
        return new OtlpHttpConfiguration(configuration.getConfiguration("opentelemetry.exporters.otlp.http"));
    }

    public Optional<String> getCompression()
    {
        return configuration.getString("compression");
    }

    public Map<String, String> getHeaders(Secrets secrets) {
        return configuration.getObjectAs("headers", JsonElement.toMap(jn -> secrets.getSecretString(jn.textValue())))
                .orElse(Collections.emptyMap());
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

    public String getLogsEndpoint()
    {
        return configuration.getString("logsEndpoint").orElse("http://localhost:4318/v1/logs");
    }

    public String getTracesEndpoint()
    {
        return configuration.getString("tracesEndpoint").orElse("http://localhost:4318/v1/traces");
    }

    public String getMetricsEndpoint()
    {
        return configuration.getString("metricsEndpoint").orElse("http://localhost:4318/v1/metrics");
    }

}
