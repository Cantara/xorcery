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
package dev.xorcery.domainevents.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.json.JsonElement;
import dev.xorcery.metadata.Metadata;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static dev.xorcery.domainevents.api.Model.JsonSystemEventModel.attributes;
import static dev.xorcery.domainevents.api.Model.JsonSystemEventModel.event;
import static java.util.Objects.requireNonNull;

/**
 * These events signify changes in the system. These can be injected in an event stream to signal non-domain changes,
 * such as streams being deleted.
 */
public record JsonSystemEvent(ObjectNode json)
    implements JsonElement, DomainEvent
{
    static final Map<Class<?>, Function<Object, JsonNode>> TO_JSON =
            Map.of(
                    String.class, v -> JsonNodeFactory.instance.textNode(v.toString()),
                    Double.class, v -> JsonNodeFactory.instance.numberNode((Double) v),
                    Float.class, v -> JsonNodeFactory.instance.numberNode((Float) v),
                    Boolean.class, v -> JsonNodeFactory.instance.booleanNode((Boolean) v),
                    Long.class, v -> JsonNodeFactory.instance.numberNode((Long) v),
                    Integer.class, v -> JsonNodeFactory.instance.numberNode((Integer) v),
                    Enum.class, v -> JsonNodeFactory.instance.textNode(((Enum<?>) v).name())
            );

    public static JsonSystemEvent.Builder systemEvent(String eventName) {
        return new JsonSystemEvent.Builder(eventName);
    }

    public record Builder(ObjectNode builder) {

        public Builder(String eventName) {
            this(JsonNodeFactory.instance.objectNode());
            requireNonNull(eventName);
            builder.set("@class", builder.textNode("JsonSystemEvent"));
            builder.set(event.name(), builder.textNode(eventName));
        }

        public Builder attribute(String name, JsonNode value) {
            JsonNode attributes = builder.get(Model.JsonSystemEventModel.attributes.name());
            if (attributes == null) {
                attributes = builder.objectNode();
                builder.set(Model.JsonSystemEventModel.attributes.name(), attributes);
            }

            ObjectNode objectNode = (ObjectNode) attributes;
            objectNode.set(name, value);

            return this;
        }

        public Builder attribute(Enum<?> name, JsonNode value) {
            return attribute(name.name(), value);
        }

        public Builder attribute(String name, Object value) {
            if (value == null) {
                attribute(name, NullNode.getInstance());
            } else {
                attribute(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public Builder attribute(Enum<?> name, Object value) {
            return attribute(name.name(), value);
        }

        public Builder addMetadata(String key, JsonNode value) {

            ObjectNode metadata = (ObjectNode) builder.get(Model.JsonSystemEventModel.metadata.name());
            if (metadata == null) {
                metadata = builder.objectNode();
                builder.set(Model.JsonSystemEventModel.metadata.name(), metadata);
            }

            metadata.set(key, value);

            return this;
        }

        public Builder addMetadata(String name, Object value) {
            if (value == null) {
                addMetadata(name, NullNode.getInstance());
            } else {
                addMetadata(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public Builder addMetadata(Enum<?> name, Object value) {
            return addMetadata(name.name(), value);
        }

        public Builder addMetadata(String name, Optional<?> optional) {
            if (optional.isEmpty()) {
                return this;
            }
            var value = optional.get();
            return value instanceof JsonNode ?
                    addMetadata(name, (JsonNode) value) :
                    addMetadata(name, value);
        }

        public Builder addMetadata(Enum<?> name, Optional<?> optional) {
            return addMetadata(name.name(), optional);
        }


        public JsonSystemEvent build() {
            return new JsonSystemEvent(builder);
        }
    }

    @JsonCreator(mode = DELEGATING)
    public JsonSystemEvent {
    }

    @JsonValue
    public ObjectNode event() {
        return json;
    }

    public String getName() {
        return json.path("event").asText();
    }

    public Map<String, JsonNode> getAttributes() {
        ObjectNode attrs = (ObjectNode) json.get(attributes.name());
        if (attrs == null)
            return Collections.emptyMap();
        else
            return JsonElement.asMap(attrs);
    }

    public Optional<Metadata> getMetadata() {
        ObjectNode metadata = (ObjectNode) json.get(Model.JsonSystemEventModel.metadata.name());
        if (metadata == null)
            return Optional.empty();
        else
            return Optional.of(new Metadata(metadata));
    }
}
