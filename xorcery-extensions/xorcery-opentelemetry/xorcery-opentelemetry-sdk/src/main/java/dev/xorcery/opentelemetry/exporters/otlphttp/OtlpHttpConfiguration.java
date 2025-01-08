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
package dev.xorcery.opentelemetry.exporters.otlphttp;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;
import dev.xorcery.secrets.Secrets;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public record OtlpHttpConfiguration(Configuration configuration) {

    public static OtlpHttpConfiguration get(Configuration configuration)
    {
        return new OtlpHttpConfiguration(configuration.getConfiguration("opentelemetry.exporters.otlp.http"));
    }

    public Duration getConnectTimeout() {
        return Duration.parse("PT" + configuration.getString("connectTimeout").orElse("5s"));
    }

    public Duration getTimeout() {
        return Duration.parse("PT" + configuration.getString("timeout").orElse("10s"));
    }

    public Optional<String> getCompression()
    {
        return configuration.getString("compression");
    }

    public Map<String, String> getHeaders(Secrets secrets) {
        return configuration.getObjectAs("headers", JsonElement.toMap(jn -> secrets.getSecretString(jn.textValue())))
                .orElse(Collections.emptyMap());
    }

    public AggregationTemporalities getAggregationTemporality() {
        return configuration.getEnum("aggregationTemporality", AggregationTemporalities.class).orElse(AggregationTemporalities.alwaysCumulative);
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

    public enum AggregationTemporalities
    {
        alwaysCumulative,
        deltaPreferred,
        lowMemory
    }
}
