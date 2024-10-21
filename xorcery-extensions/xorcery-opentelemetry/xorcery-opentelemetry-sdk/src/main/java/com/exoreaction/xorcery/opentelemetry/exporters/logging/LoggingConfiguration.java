/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.opentelemetry.exporters.logging;

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
