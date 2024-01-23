package com.exoreaction.xorcery.opentelemetry.jmx;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.lang.Classes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public record OpenTelemetryJMXConfiguration(Configuration context) {

    public static OpenTelemetryJMXConfiguration get(Configuration configuration) {
        return new OpenTelemetryJMXConfiguration(configuration.getConfiguration("opentelemetry.instrumentations.jmx"));
    }

    public Map<String, JmxAttributeConfiguration> getAttributes() {
        return context.getObjectAs("attributes", on ->
                JsonElement.toMap(on, Classes.<JsonNode, ObjectNode>cast(ObjectNode.class).andThen(JmxAttributeConfiguration::new))).orElse(Collections.emptyMap());
    }

    public record JmxAttributeConfiguration(ContainerNode<?> json)
            implements JsonElement {

        public String getObjectName()
        {
            return getString("objectName").orElseThrow(Configuration.missing("objectName"));
        }

        public String getAttributeName()
        {
            return getString("attribute").orElseThrow(Configuration.missing("attribute"));
        }

        public String getUnit()
        {
            return getString("unit").orElse("");
        }

        public String getType()
        {
            return getString("type").orElse("Gauge");
        }

        public Optional<String> getDescription() {
            return getString("description");
        }
    }
}
