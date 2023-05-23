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
package com.exoreaction.xorcery.jsonapi;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.JsonElement;
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

    public boolean isEmpty()
    {
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
}
