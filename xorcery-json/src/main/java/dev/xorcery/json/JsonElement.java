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
package dev.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import dev.xorcery.collections.Element;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;

/**
 * Marker interface with helper methods to denote a model that is backed by JSON.
 *
 * <p>This interface provides a comprehensive set of utility methods for working with JSON data
 * using Jackson's JsonNode as the underlying representation. It extends the {@link Element}
 * interface to provide additional JSON-specific functionality.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li>Type-safe access to JSON values with optional wrapping</li>
 *   <li>Hierarchical property lookup with dot notation</li>
 *   <li>Collection and array processing utilities</li>
 *   <li>Conversion between JSON and Java objects</li>
 *   <li>Enum handling and falsy value evaluation</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public record MyModel(ObjectNode json) implements JsonElement {
 *     // Use inherited methods
 *     public Optional<String> getName() {
 *         return getString("name");
 *     }
 *
 *     public List<String> getTags() {
 *         return getListAs("tags", JsonNode::asText).orElse(Collections.emptyList());
 *     }
 * }
 * }</pre>
 *
 * @author rickardoberg
 * @since 1.0.0
 */
public interface JsonElement
        extends Element
{
    /**
     * Returns the underlying JSON representation of this element.
     *
     * @return the JsonNode representing this element's data
     */
    JsonNode json();

    /**
     * Returns this element's JSON as an ObjectNode if it represents an object.
     *
     * @return the ObjectNode if this element is a JSON object, null otherwise
     */
    default ObjectNode object() {
        if (json() instanceof ObjectNode object)
            return object;
        else
            return null;
    }

    /**
     * Returns this element's JSON as an ArrayNode if it represents an array.
     *
     * @return the ArrayNode if this element is a JSON array, null otherwise
     */
    default ArrayNode array() {
        if (json() instanceof ArrayNode array)
            return array;
        else
            return null;
    }

    /**
     * Checks if this element has a property with the specified name.
     * Supports dot notation for nested property access.
     *
     * @param name the property name, may use dot notation (e.g., "user.name")
     * @return true if the property exists and is not null, false otherwise
     */
    default boolean has(String name) {
        return lookup(object(), name).isPresent();
    }

    /**
     * Retrieves a JSON node by property name.
     * Supports dot notation for nested property access.
     *
     * @param name the property name, may use dot notation (e.g., "user.address.city")
     * @return an Optional containing the JsonNode if found, empty otherwise
     */
    default Optional<JsonNode> getJson(String name) {
        return lookup(object(), name);
    }

    /**
     * Retrieves a JSON object property and maps it using the provided mapper function.
     *
     * @param <T> the type of the mapped result
     * @param name the property name
     * @param mapper function to convert ObjectNode to desired type
     * @return an Optional containing the mapped result if the property exists as an object
     */
    default <T> Optional<T> getObjectAs(String name, Function<ObjectNode, T> mapper) {
        return getJson(name).map(ObjectNode.class::cast).map(mapper);
    }

    /**
     * Retrieves a JSON array property as an iterable.
     *
     * @param name the property name
     * @return an Optional containing an Iterable of JsonNodes if the property is an array
     */
    default Optional<Iterable<JsonNode>> getList(String name) {
        return getJson(name).map(ArrayNode.class::cast);
    }

    /**
     * Retrieves a JSON array property and maps each element using the provided mapper function.
     *
     * @param <T> the type of the mapped elements
     * @param name the property name
     * @param mapper function to convert each JsonNode element to desired type
     * @return an Optional containing a List of mapped elements if the property is an array
     */
    default <T> Optional<List<T>> getListAs(String name, Function<JsonNode, T> mapper) {
        return getJson(name).map(ArrayNode.class::cast).map(array -> getValuesAs(array, mapper));
    }

    /**
     * Retrieves a JSON array property containing objects and maps each object using the provided mapper function.
     *
     * @param <T> the type of the mapped elements
     * @param name the property name
     * @param mapper function to convert each ObjectNode element to desired type
     * @return an Optional containing a List of mapped objects if the property is an array of objects
     */
    default <T> Optional<List<T>> getObjectListAs(String name, Function<ObjectNode, T> mapper) {
        return getJson(name).map(ArrayNode.class::cast).map(array -> getValuesAs(array, mapper));
    }

    /**
     * Converts this JSON element to a Map representation.
     * Only works if the underlying JSON is an object.
     *
     * @return a Map containing the JSON object's key-value pairs, empty map if not an object
     */
    default Map<String, JsonNode> asMap()
    {
        if (json() instanceof ObjectNode on)
            return toMap(on, Function.identity());
        return Collections.emptyMap();
    }

    /**
     * Returns a pretty-printed JSON string representation of this element.
     *
     * @return formatted JSON string
     */
    default String toJsonString() {
        return json().toPrettyString();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Retrieves a property value and converts it to a Java object using automatic type conversion.</p>
     *
     * @param <T> the expected return type
     * @param name the property name, supports dot notation
     * @return an Optional containing the converted value, empty if property doesn't exist
     */
    @Override
    default <T> Optional<T> get(String name) {
        return lookup(object(), name).map(JsonElement::toObject);
    }

    /**
     * Evaluates a property as a "falsy" value using JavaScript-like truthiness rules.
     *
     * <p>Falsy evaluation rules:</p>
     * <ul>
     *   <li>Arrays: false if empty, true if has elements</li>
     *   <li>Binary: always true</li>
     *   <li>Boolean: the boolean value itself</li>
     *   <li>Missing/Null: false</li>
     *   <li>Number: false if zero, true otherwise</li>
     *   <li>Object/POJO: always true</li>
     *   <li>String: false if empty or "false" (case-insensitive), true otherwise</li>
     * </ul>
     *
     * @param name the property name
     * @return the falsy evaluation result, false if property doesn't exist
     */
    default Boolean getFalsy(String name) {
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
                    case STRING -> !value.textValue().equals("") && !value.textValue().equalsIgnoreCase("false");
                }).orElse(false);
    }

    /**
     * Retrieves a string property value as an enum constant.
     *
     * @param <T> the enum type
     * @param name the property name
     * @param enumClass the Class object of the enum type
     * @return an Optional containing the enum constant if the property exists and is valid
     */
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

    // Static Helper Methods

    /**
     * Converts a collection of JsonElement objects to a JSON array.
     *
     * @param elements the collection of JsonElement objects
     * @return an ArrayNode containing the JSON representation of all elements
     */
    static ArrayNode toArray(Collection<? extends JsonElement> elements) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(elements.size());
        for (JsonElement element : elements) {
            array.add(element.json());
        }
        return array;
    }

    /**
     * Converts multiple JsonElement objects to a JSON array.
     *
     * @param elements the JsonElement objects
     * @return an ArrayNode containing the JSON representation of all elements
     */
    static ArrayNode toArray(JsonElement... elements) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(elements.length);
        for (JsonElement element : elements) {
            array.add(element.json());
        }
        return array;
    }

    /**
     * Converts multiple string values to a JSON array.
     *
     * @param values the string values
     * @return an ArrayNode containing text nodes for all values
     */
    static ArrayNode toArray(String... values) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(values.length);
        for (String value : values) {
            array.add(array.textNode(value));
        }
        return array;
    }

    /**
     * Returns a Collector that accumulates JsonNode elements into an ArrayNode.
     * Null nodes are ignored during collection.
     *
     * @return a Collector for creating ArrayNodes from streams of JsonNodes
     */
    static Collector<JsonNode, ArrayNode, ArrayNode> toArray() {
        return Collector.of(JsonNodeFactory.instance::arrayNode,
                (array, node) -> {
                    if (node != null) array.add(node);
                }, (builder1, builder2) -> builder1, Function.identity());
    }

    /**
     * Maps elements from a container node to a list using the provided mapping function.
     *
     * @param <T> the target type after mapping
     * @param <U> the source JsonNode type
     * @param arrayNode the container node (array or object)
     * @param mapFunction function to convert each element
     * @return a List containing the mapped elements
     */
    static <T, U extends JsonNode> List<T> getValuesAs(ContainerNode<?> arrayNode, Function<U, T> mapFunction) {
        List<T> list = new ArrayList<>(arrayNode.size());
        for (JsonNode jsonNode : arrayNode) {
            list.add(mapFunction.apply((U) jsonNode));
        }
        return list;
    }

    /**
     * Converts an ObjectNode to a Map with String keys and JsonNode values.
     *
     * @param objectNode the ObjectNode to convert
     * @return a Map representation of the object
     */
    static Map<String, JsonNode> asMap(ObjectNode objectNode) {
        return toMap(objectNode, Function.identity());
    }

    /**
     * Converts an ObjectNode to a Map, applying a value mapping function.
     *
     * @param <T> the type of mapped values
     * @param object the ObjectNode to convert
     * @param valueMapper function to convert JsonNode values to target type
     * @return a Map with String keys and mapped values
     */
    static <T> Map<String, T> toMap(ObjectNode object, Function<JsonNode, T> valueMapper) {
        Map<String, T> result = new LinkedHashMap<>(object.size());
        Iterator<Map.Entry<String, JsonNode>> fields = object.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            result.put(next.getKey(), valueMapper.apply(next.getValue()));
        }
        return result;
    }

    /**
     * Returns a function that converts ObjectNodes to Maps with mapped values.
     *
     * @param <T> the type of mapped values
     * @param valueMapper function to convert JsonNode values to target type
     * @return a function that converts ObjectNodes to Maps
     */
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

    /**
     * Converts a JsonNode to a corresponding Java object.
     *
     * <p>Conversion rules:</p>
     * <ul>
     *   <li>Arrays become Lists</li>
     *   <li>Objects become Maps</li>
     *   <li>Strings remain Strings</li>
     *   <li>Numbers become Number instances</li>
     *   <li>Booleans become Boolean instances</li>
     *   <li>Null/Missing/Binary/POJO become null</li>
     * </ul>
     *
     * @param <T> the expected return type
     * @param json the JsonNode to convert
     * @return the converted Java object
     */
    static <T> T toObject(JsonNode json)
    {
        return (T) switch (json.getNodeType()) {
            case ARRAY -> toList((ArrayNode) json);
            case OBJECT -> toMap((ObjectNode) json);
            case STRING -> json.textValue();
            case NUMBER -> json.numberValue();
            case BINARY -> null;
            case BOOLEAN -> json.booleanValue();
            case MISSING -> null;
            case NULL -> null;
            case POJO -> null;
        };
    }

    /**
     * Converts an ObjectNode to a Map&lt;String, Object&gt; with recursive conversion.
     *
     * @param json the ObjectNode to convert
     * @return a Map containing converted values
     */
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

    /**
     * Converts an ArrayNode to a List&lt;Object&gt; with recursive conversion.
     *
     * @param json the ArrayNode to convert
     * @return a List containing converted values
     */
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

    /**
     * Converts an ObjectNode to a flat Map using dot notation for nested keys.
     *
     * <p>For example, a nested object like:</p>
     * <pre>{@code
     * {
     *   "user": {
     *     "name": "John",
     *     "address": {
     *       "city": "New York"
     *     }
     *   }
     * }
     * }</pre>
     *
     * <p>Would become a flat map with keys: "user.name" and "user.address.city"</p>
     *
     * @param <T> the type of mapped values
     * @param object the ObjectNode to flatten
     * @param mapper function to convert leaf JsonNode values
     * @return a flat Map with dot-notation keys
     */
    static <T> Map<String, T> toFlatMap(ObjectNode object, Function<JsonNode, T> mapper) {
        Map<String, T> result = new LinkedHashMap<>(object.size());
        toFlatMap(result, "", object, mapper);
        return result;
    }

    /**
     * Returns a function that converts ObjectNodes to flat Maps using dot notation.
     *
     * @param <T> the type of mapped values
     * @param mapper function to convert leaf JsonNode values
     * @return a function that creates flat Maps from ObjectNodes
     */
    static <T> Function<ObjectNode, Map<String, T>> toFlatMap(Function<JsonNode, T> mapper) {
        return object ->
        {
            Map<String, T> result = new LinkedHashMap<>(object.size());
            toFlatMap(result, "", object, mapper);
            return result;
        };
    }

    /**
     * Recursively flattens an ObjectNode into a Map using dot notation for nested keys.
     * This is a helper method for the public toFlatMap methods.
     *
     * @param <T> the type of mapped values
     * @param result the target Map to populate
     * @param prefix the current key prefix (for nested objects)
     * @param object the ObjectNode to process
     * @param mapper function to convert leaf values
     */
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

    /**
     * Looks up a value in a container using property name with optional dot notation.
     *
     * <p>Supports hierarchical lookup using dot notation (e.g., "user.address.city").
     * Also supports property names starting with a dot to avoid the dot-notation parsing.</p>
     *
     * @param c the container to search in (ObjectNode or ArrayNode)
     * @param name the property name, may include dots for nested access
     * @return an Optional containing the found value, empty if not found or null
     */
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