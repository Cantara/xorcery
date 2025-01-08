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
package dev.xorcery.opentelemetry.sdk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;
import dev.xorcery.json.JsonElement;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.ResourceAttributes;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OpenTelemetryConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public static OpenTelemetryConfiguration get(Configuration configuration)
    {
        return new OpenTelemetryConfiguration(configuration.getConfiguration("opentelemetry"));
    }

    public boolean isInstall()
    {
        return context.getBoolean("install").orElse(false);
    }

    public Map<AttributeKey<Object>, Object> getResource() {
        return context.getJson("resource")
                .map(ObjectNode.class::cast)
                .map(JsonElement::toMap)
                .map(map -> map.entrySet().stream().map(this::updateEntry).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .orElse(Collections.<AttributeKey<Object>, Object>emptyMap());
    }

    // Metrics
    public Duration getMetricReaderInterval() {
        return Duration.parse("PT" + context.getString("meters.interval").orElse("30s"));
    }

    public List<String> getIncludedMeters()
    {
        return context.getListAs("meters.includes", JsonNode::asText).orElse(Collections.emptyList());
    }

    public List<String> getExcludedMeters()
    {
        return context.getListAs("meters.excludes", JsonNode::asText).orElse(Collections.emptyList());
    }

    // Logging
    public Duration getLoggingScheduleDelay() {
        return Duration.parse("PT" + context.getString("logging.scheduleDelay").orElse("5s"));
    }

    public Duration getLoggingExporterTimeout() {
        return Duration.parse("PT" + context.getString("logging.exporterTimeout").orElse("1h"));
    }
    public int getLoggingMaxExportBatchSize() {
        return context.getInteger("logging.maxExportBatchSize").orElse(1000);
    }

    public int getLoggingMaxQueueSize() {
        return context.getInteger("logging.maxQueueSize").orElse(10000);
    }

    // Spans
    public Duration getSpanScheduleDelay() {
        return Duration.parse("PT" + context.getString("spans.scheduleDelay").orElse("5s"));
    }

    public Duration getSpanExporterTimeout() {
        return Duration.parse("PT" + context.getString("spans.exporterTimeout").orElse("1h"));
    }
    public int getSpanMaxExportBatchSize() {
        return context.getInteger("spans.maxExportBatchSize").orElse(1000);
    }

    public int getSpanMaxQueueSize() {
        return context.getInteger("spans.maxQueueSize").orElse(10000);
    }

    private Map.Entry<AttributeKey<Object>, Object> updateEntry(Map.Entry<String, Object> entry) {
        try {
            return Map.entry((AttributeKey<Object>)ResourceAttributes.class.getField(entry.getKey().toUpperCase().replace('.','_')).get(null), entry.getValue());
        } catch (Throwable e) {
            throw new IllegalArgumentException("No such resource attribute:"+entry.getKey(), e);
        }
    }
}
