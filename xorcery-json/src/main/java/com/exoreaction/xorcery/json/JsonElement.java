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
package com.exoreaction.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Marker interface with helper methods to denote a model that is backed by JSON.
 *
 * @author rickardoberg
 */
public interface JsonElement {
    JsonNode json();

    default ObjectNode object() {
        if (json() instanceof ObjectNode object)
            return object;
        else
            return null;
    }

    default ArrayNode array() {
        if (json() instanceof ArrayNode array)
            return array;
        else
            return null;
    }

    default boolean has(String name) {
        return lookup(object(), name).isPresent();
    }

    default Optional<JsonNode> getJson(String name) {
        return lookup(object(), name);
    }

    default Optional<String> getString(String name) {
        return getJson(name).map(JsonNode::textValue);
    }

    default Optional<URI> getURI(String name) {

        return getString(name).map(s -> s.replace('\\','/').replace(' ', '+')).map(URI::create);
    }

    default Optional<Integer> getInteger(String name) {
        return getJson(name).flatMap(value ->
        {
            if (value instanceof NumericNode number) {
                return Optional.of(number.intValue());
            }
            try {
                return Optional.of(Integer.valueOf(value.asText()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    default Optional<Long> getLong(String name) {
        return getJson(name).flatMap(value ->
        {
            if (value instanceof NumericNode number) {
                return Optional.of(number.longValue());
            }
            try {
                return Optional.of(Long.valueOf(value.asText()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
    }

    default Optional<Boolean> getBoolean(String name) {
        return getJson(name).map(JsonNode::asBoolean);
    }

    default Optional<Boolean> getFalsy(String name) {
        return getJson(name).map(value ->
                switch (value.getNodeType()) {
                    case ARRAY -> !value.isEmpty();
                    case BINARY -> true;
                    case BOOLEAN -> value.booleanValue();
                    case MISSING -> false;
                    case NULL -> false;
                    case NUMBER -> value.numberValue().longValue() != 0;
                    case OBJECT -> true;
                    case POJO -> true;
                    case STRING -> !value.textValue().equals("");
                });
    }

    default <T> Optional<T> getObjectAs(String name, Function<ObjectNode, T> mapper) {
        return getJson(name).map(ObjectNode.class::cast).map(mapper);
    }

    default Optional<Iterable<JsonNode>> getList(String name) {
        return getJson(name).map(ArrayNode.class::cast);
    }

    default <T> Optional<List<T>> getListAs(String name, Function<JsonNode, T> mapper) {
        return getJson(name).map(ArrayNode.class::cast).map(array -> getValuesAs(array, mapper));
    }

    default <T> Optional<List<T>> getObjectListAs(String name, Function<ObjectNode, T> mapper) {
        return getJson(name).map(ArrayNode.class::cast).map(array -> getValuesAs(array, mapper));
    }

    default <T extends Enum<T>> Optional<T> getEnum(String name, Class<T> enumClass) {
        Optional<String> value = getString(name);

        if (value.isPresent()) {
            try {
                return Optional.of(Enum.valueOf(enumClass, value.get()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    default Map<String, JsonNode> asMap()
    {
        if (json() instanceof ObjectNode on)
            return toMap(on, Function.identity());
        return Collections.emptyMap();
    }

    default String toJsonString() {
        return json().toPrettyString();
    }

    static ArrayNode toArray(Collection<? extends JsonElement> elements) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(elements.size());
        for (JsonElement element : elements) {
            array.add(element.json());
        }
        return array;
    }

    static ArrayNode toArray(JsonElement... elements) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(elements.length);
        for (JsonElement element : elements) {
            array.add(element.json());
        }
        return array;
    }

    static ArrayNode toArray(String... values) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(values.length);
        for (String value : values) {
            array.add(array.textNode(value));
        }
        return array;
    }

    static Collector<JsonNode, ArrayNode, ArrayNode> toArray() {
        return Collector.of(JsonNodeFactory.instance::arrayNode,
                (array, node) -> {
                    if (node != null) array.add(node);
                }, (builder1, builder2) -> builder1, Function.identity());
    }

    static <T, U extends JsonNode> List<T> getValuesAs(ContainerNode<?> arrayNode, Function<U, T> mapFunction) {
        List<T> list = new ArrayList<>(arrayNode.size());
        for (JsonNode jsonNode : arrayNode) {
            list.add(mapFunction.apply((U) jsonNode));
        }
        return list;
    }

    static Map<String, JsonNode> asMap(ObjectNode objectNode) {
        return toMap(objectNode, Function.identity());
    }

    static <T> Map<String, T> toMap(ObjectNode object, Function<JsonNode, T> valueMapper) {
        Map<String, T> result = new LinkedHashMap<>(object.size());
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            result.put(next.getKey(), valueMapper.apply(next.getValue()));
        }
        return result;
    }

    static <T> Function<ObjectNode, Map<String, T>> toMap(Function<JsonNode, T> valueMapper) {
        return object ->
        {
            Map<String, T> result = new LinkedHashMap<>(object.size());
            Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> next = fields.next();
                result.put(next.getKey(), valueMapper.apply(next.getValue()));
            }
            return result;
        };
    }

    static Map<String, Object> toMap(ObjectNode json) {
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        Map<String, Object> map = new HashMap<>(json.size());
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();

            map.put(next.getKey(), switch (next.getValue().getNodeType()) {
                case ARRAY -> toList((ArrayNode) next.getValue());
                case OBJECT -> toMap((ObjectNode) next.getValue());
                case STRING -> next.getValue().textValue();
                case NUMBER -> next.getValue().numberValue();
                case BINARY -> null;
                case BOOLEAN -> next.getValue().booleanValue();
                case MISSING -> null;
                case NULL -> null;
                case POJO -> null;
            });
        }
        return map;
    }

    static List<Object> toList(ArrayNode json) {
        Iterator<JsonNode> fields = json.elements();
        List<Object> list = new ArrayList<>(json.size());
        while (fields.hasNext()) {
            JsonNode next = fields.next();

            list.add(switch (next.getNodeType()) {
                case ARRAY -> toList((ArrayNode) next);
                case OBJECT -> toMap((ObjectNode) next);
                case STRING -> next.textValue();
                case NUMBER -> next.numberValue();
                case BINARY -> null;
                case BOOLEAN -> Boolean.TRUE;
                case MISSING -> null;
                case NULL -> null;
                case POJO -> null;
            });
        }
        return list;
    }

    static <T> Map<String, T> toFlatMap(ObjectNode object, Function<JsonNode, T> mapper) {
        Map<String, T> result = new LinkedHashMap<>(object.size());
        toFlatMap(result, "", object, mapper);
        return result;
    }

    static <T> Function<ObjectNode, Map<String, T>> toFlatMap(Function<JsonNode, T> mapper) {
        return object ->
        {
            Map<String, T> result = new LinkedHashMap<>(object.size());
            toFlatMap(result, "", object, mapper);
            return result;
        };
    }

    static <T> void toFlatMap(Map<String, T> result, String prefix, ObjectNode object, Function<JsonNode, T> mapper) {
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            if (next.getValue().getNodeType().equals(JsonNodeType.OBJECT)) {
                toFlatMap(result, prefix + next.getKey() + ".", (ObjectNode) next.getValue(), mapper);
            } else {
                JsonNode value = next.getValue();
                T mappedValue = mapper.apply(value);
                result.put(prefix + next.getKey(), mappedValue);
            }
        }
    }

    static Optional<JsonNode> lookup(ContainerNode<?> c, String name) {
        if (!name.startsWith(".") && name.indexOf('.') != -1) {
            String[] names = name.split("\\.");
            for (int i = 0; i < names.length - 1; i++) {
                JsonNode node = c.get(names[i]);
                if (node instanceof ContainerNode)
                    c = (ContainerNode<?>) node;
                else
                    return Optional.empty();
            }
            JsonNode value = c.get(names[names.length - 1]);
            return value instanceof NullNode ? Optional.empty() : Optional.ofNullable(value);
        } else {
            if (name.startsWith(".")) {
                name = name.substring(1);
            }
            JsonNode value = c.get(name);
            return value instanceof NullNode ? Optional.empty() : Optional.ofNullable(value);
        }
    }
}
