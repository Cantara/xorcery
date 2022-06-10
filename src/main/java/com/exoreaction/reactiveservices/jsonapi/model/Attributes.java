package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.With;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.time.Period;
import java.util.*;

/**
 * @author rickardoberg
 */

public record Attributes(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
        implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder attribute(String name, Object value) {
            builder.set(name, getValue(value));
            return this;
        }

        public Builder attribute(Enum<?> name, Object value) {
            return attribute(name.name(), value);
        }

        public Builder attribute(String name, JsonNode value) {
            builder.set(name, value);
            return this;
        }

        public Builder attributes(ObjectNode objectNode) {
            objectNode.fields().forEachRemaining(entry -> builder.set(entry.getKey(), entry.getValue()));
            return this;
        }

        private JsonNode getValue(Object value) {
            if (value == null) {
                return NullNode.instance;
            } else {
                if (value instanceof String v) {
                    return builder.textNode(v);
                } else if (value instanceof Boolean v) {
                    return builder.booleanNode(v);
                } else if (value instanceof Long v) {
                    return builder.numberNode(v);
                } else if (value instanceof Integer v) {
                    return builder.numberNode(v);
                } else if (value instanceof BigDecimal v) {
                    return builder.numberNode(v);
                } else if (value instanceof Double v) {
                    return builder.numberNode(v);
                } else if (Double.TYPE.isInstance(value)) {
                    return builder.numberNode((Double) value);
                } else if (value instanceof Float v) {
                    return builder.numberNode(v);
                } else if (value instanceof Enum v) {
                    return builder.textNode(v.name());
                } else if (value instanceof Map) {
                    Set<Map.Entry<Object, Object>> set = ((Map<Object, Object>) value).entrySet();
                    ObjectNode map = builder.objectNode();
                    for (Map.Entry<Object, Object> entry : set) {
                        map.set(entry.getKey().toString(), getValue(entry.getValue()));
                    }
                    return map;
                } else if (value instanceof Period) {
                    return builder.textNode(value.toString());
                } else if (value.getClass().isArray()) {
                    ArrayNode arrayBuilder = builder.arrayNode();

                    Object[] array = (Object[]) value;
                    for (Object instance : array) {
                        arrayBuilder.add(getValue(instance));
                    }
                    return arrayBuilder;
                } else if (value instanceof Collection) {
                    ArrayNode arrayBuilder = builder.arrayNode();
                    Collection collection = (Collection) value;
                    for (Object instance : collection) {
                        arrayBuilder.add(getValue(instance));
                    }
                    return arrayBuilder;
                } else {
                    throw new IllegalArgumentException("Not a valid value type:" + value.getClass());
                }
            }
        }

        public Attributes build() {
            return new Attributes(builder);
        }
    }

    public Optional<JsonNode> getAttribute(String name) {
        return Optional.ofNullable(object().get(name));
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new HashMap<>(json().size());
        Iterator<Map.Entry<String, JsonNode>> fields = object().fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            String mapValue = switch (value.getNodeType()) {
                case ARRAY -> null; // We could serialize these to string or BASE64
                case OBJECT -> null;
                case STRING -> value.textValue();
                case NUMBER -> value.textValue();
                case BINARY -> null;
                case BOOLEAN -> value.textValue();
                case MISSING -> null;
                case NULL -> null;
                case POJO -> null;
            };
            if (mapValue != null)
                map.put(entry.getKey(), mapValue);
        }
        return map;
    }
}
