package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
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

public final class Attributes
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Attributes(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

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
                if (value instanceof String) {
                    return builder.textNode((String) value);
                } else if (value instanceof Boolean) {
                    return builder.booleanNode((Boolean) value);
                } else if (value instanceof Long) {
                    return builder.numberNode((Long) value);
                } else if (value instanceof Integer) {
                    return builder.numberNode((Integer) value);
                } else if (value instanceof BigDecimal) {
                    return builder.numberNode((BigDecimal) value);
                } else if (value instanceof Double) {
                    return builder.numberNode((Double) value);
                } else if (Double.TYPE.isInstance(value)) {
                    return builder.numberNode((Double) value);
                } else if (value instanceof Float) {
                    return builder.numberNode((Float) value);
                } else if (value instanceof Enum) {
                    return builder.textNode(((Enum) value).name());
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

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
        }

    }

    public boolean isEmpty() {
        return object().isEmpty();
    }

    public Optional<JsonNode> getAttribute(String name) {
        return Optional.ofNullable(object().get(name));
    }

    public Map<String, String> toMap() {
        Map<String, String> map = new LinkedHashMap<>(json().size());
        Iterator<Map.Entry<String, JsonNode>> fields = object().fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            String mapValue = value.toPrettyString();
            map.put(entry.getKey(), mapValue);
        }
        return map;
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Attributes) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Attributes[" +
               "json=" + json + ']';
    }

}
