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
    ContainerNode<?> json();

    default ObjectNode object() {
        ContainerNode<?> json = json();
        if (json instanceof ObjectNode object)
            return object;
        else
            return null;
    }

    default ArrayNode array() {
        ContainerNode<?> json = json();
        if (json instanceof ArrayNode array)
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

        return getString(name).map(URI::create);
    }

    default Optional<Integer> getInteger(String name) {
        return getJson(name).flatMap(value ->
        {
            if (value instanceof NumericNode number) {
                return Optional.of(number.intValue());
            } else {
                return Optional.empty();
            }
        });
    }

    default Optional<Long> getLong(String name) {
        return getJson(name).flatMap(value ->
        {
            if (value instanceof NumericNode number) {
                return Optional.of(number.longValue());
            } else {
                return Optional.empty();
            }
        });
    }

    default Optional<Boolean> getBoolean(String name) {
        return getJson(name).map(JsonNode::asBoolean);
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

    default Map<String, JsonNode> asMap() {
        return JsonElement.asMap(object());
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
        Map<String, JsonNode> map = new HashMap<>(objectNode.size());
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    static <T> Map<String, T> toMap(ObjectNode object, Function<JsonNode, T> mapper) {
        Map<String, T> result = new LinkedHashMap<>(object.size());
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            result.put(next.getKey(), mapper.apply(next.getValue()));
        }
        return result;
    }

    static <T> Map<String, T> toFlatMap(ObjectNode object, Function<JsonNode, T> mapper) {
        Map<String, T> result = new LinkedHashMap<>(object.size());
        toFlatMap(result, "", object, mapper);
        return result;
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

    static Optional<JsonNode> lookup(ObjectNode c, String name) {
        if (name.indexOf('.') != -1) {
            String[] names = name.split("\\.");
            for (int i = 0; i < names.length - 1; i++) {
                JsonNode node = c.get(names[i]);
                if (node instanceof ObjectNode)
                    c = (ObjectNode) node;
                else
                    return Optional.empty();
            }
            JsonNode value = c.get(names[names.length - 1]);
            return value instanceof NullNode ? Optional.empty() : Optional.ofNullable(value);
        } else {
            JsonNode value = c.get(name);
            return value instanceof NullNode ? Optional.empty() : Optional.ofNullable(value);
        }
    }
}
