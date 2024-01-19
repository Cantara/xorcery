package com.exoreaction.xorcery.opentelemetry.sdk;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.semconv.ResourceAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public record OpenTelemetryConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public static OpenTelemetryConfiguration get(Configuration configuration)
    {
        return new OpenTelemetryConfiguration(configuration.getConfiguration("opentelemetry"));
    }

    public Map<AttributeKey<Object>, Object> getResource() {
        return context.getJson("resource")
                .map(ObjectNode.class::cast)
                .map(JsonElement::toMap)
                .map(map -> map.entrySet().stream().map(this::updateEntry).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .orElse(Collections.<AttributeKey<Object>, Object>emptyMap());
    }

    private Map.Entry<AttributeKey<Object>, Object> updateEntry(Map.Entry<String, Object> entry) {
        try {
            return Map.entry((AttributeKey<Object>)ResourceAttributes.class.getField(entry.getKey().toUpperCase().replace('.','_')).get(null), entry.getValue());
        } catch (Throwable e) {
            throw new IllegalArgumentException("No such resource attribute:"+entry.getKey(), e);
        }
    }
}
