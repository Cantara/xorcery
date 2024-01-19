package com.exoreaction.xorcery.opentelemetry;

import com.exoreaction.xorcery.builders.WithContext;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */

public interface OpenTelemetryElement
    extends WithContext<Configuration>
{
    default Map<String,String> getAttributes()
    {
        Map<String, String> attributes = context()
                .getObjectAs("attributes", JsonElement.toMap(JsonNode::asText))
                .orElse(Collections.emptyMap());
        attributes.entrySet().removeIf(entry -> entry.getValue() == null);
        return attributes;
    }
}
