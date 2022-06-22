/*
 *  Copyright (C) 2018 Real Vision Group SEZC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.exoreaction.xorcery.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.util.Collection;
import java.util.Optional;

/**
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

    default String getString(String name) {
        return object().path(name).asText();
    }

    default Optional<String> getOptionalString(String name) {
        return Optional.ofNullable(object().get(name)).map(JsonNode::asText);
    }

    default int getInt(String name) {
        return object().path(name).intValue();
    }

    default Optional<Integer> getOptionalInt(String name) {
        JsonNode value = object().path(name);
        if (value instanceof NumericNode number) {
            return Optional.of(number.intValue());
        } else {
            return Optional.empty();
        }
    }

    default long getLong(String name) {
        return object().path(name).longValue();
    }

    default Optional<Long> getOptionalLong(String name) {
        JsonNode value = object().path(name);
        if (value instanceof NumericNode number) {
            return Optional.of(number.longValue());
        } else {
            return Optional.empty();
        }
    }

    default boolean getBoolean(String name) {
        return object().path(name).booleanValue();
    }

    default Optional<Boolean> getOptionalBoolean(String name) {
        JsonNode value = object().path(name);
        if (value instanceof BooleanNode bool) {
            return Optional.of(bool.booleanValue());
        } else {
            return Optional.empty();
        }
    }

    default <T extends Enum<T>> T getEnum(String name, Class<T> enumClass) {
        Optional<String> value = getOptionalString(name);

        if (value.isPresent()) {
            try {
                return Enum.valueOf(enumClass, value.get());
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    default <T extends Enum<T>> Optional<T> getOptionalEnum(String name, Class<T> enumClass) {
        Optional<String> value = getOptionalString(name);

        if (value.isPresent()) {
            try {
                return Optional.of(Enum.valueOf(enumClass, value.get()));
            } catch (IllegalArgumentException e) {
                return null;
            }
        } else {
            return Optional.empty();
        }
    }

    default String toJsonString() {
        return json().toPrettyString();
    }

    static ArrayNode toArray(Collection<? extends JsonElement> elements)
    {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(elements.size());
        for (JsonElement element : elements) {
            array.add(element.json());
        }
        return array;
    }

    static ArrayNode toArray(JsonElement... elements)
    {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(elements.length);
        for (JsonElement element : elements) {
            array.add(element.json());
        }
        return array;
    }

    static ArrayNode toArray(String... values)
    {
        ArrayNode array = JsonNodeFactory.instance.arrayNode(values.length);
        for (String value : values) {
            array.add(array.textNode(value));
        }
        return array;
    }
}
