package com.exoreaction.xorcery.opentelemetry.sdk.exporters;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.LinkData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SampleRule(String name, SpanKind spanKind, List<Map.Entry<AttributeKey<?>, Object>> attributes) {

    public static SampleRule create(ObjectNode json) {
        JsonElement jsonElement = () -> json;
        List<Map.Entry<AttributeKey<?>, Object>> attributes = jsonElement
                .getObjectAs("attributes", SampleRule::toAttributes)
                .map(attr -> attr.entrySet().stream().toList())
                .orElse(null);
        String name = jsonElement.getString("name").orElse(null);
        SpanKind spanKind = jsonElement.getEnum("spanKind", SpanKind.class).orElse(null);
        return new SampleRule(name, spanKind, attributes);
    }

    private static Map<AttributeKey<?>, Object> toAttributes(ObjectNode objectNode) {

        Map<AttributeKey<?>, Object> attributes = new HashMap<>();
        for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
            switch (property.getValue().getNodeType()) {
                case BOOLEAN -> {
                    attributes.put(AttributeKey.booleanKey(property.getKey()), property.getValue().asBoolean());
                }
                case NUMBER -> {
                    if (property.getValue().isFloatingPointNumber())
                        attributes.put(AttributeKey.doubleKey(property.getKey()), property.getValue().asDouble());
                    else
                        attributes.put(AttributeKey.longKey(property.getKey()), property.getValue().asLong());
                }
                case STRING -> {
                    attributes.put(AttributeKey.stringKey(property.getKey()), property.getValue().asDouble());
                }
            }
        }
        return attributes;
    }

    public boolean matches(String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {

        if (this.name != null) {
            if (!name.equals(this.name))
                return false;
        }
        if (this.spanKind != null) {
            if (!spanKind.equals(this.spanKind))
                return false;
        }
        if (this.attributes != null) {
            for (int i = 0; i < this.attributes.size(); i++) {
                Map.Entry<AttributeKey<?>, Object> entry = this.attributes.get(i);
                Object value = attributes.get(entry.getKey());
                if (value != null) {
                    if (!value.equals(entry.getValue()))
                        return false;
                }
            }
        }
        return true;
    }
}
