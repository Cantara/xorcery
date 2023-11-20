package com.exoreaction.xorcery.domainevents.api;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel.attributes;
import static com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel.event;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;

/**
 * These events signify changes in the system. These can be injected in an event stream to signal non-domain changes,
 * such as a stream reaching the live state.
 */
public record JsonSystemEvent(ObjectNode json)
    implements JsonElement, DomainEvent
{

    public static JsonSystemEvent.Builder systemEvent(String eventName) {
        return new JsonSystemEvent.Builder(eventName);
    }

    public record Builder(ObjectNode builder) {

        public Builder(String eventName) {
            this(JsonNodeFactory.instance.objectNode());
            builder.set(event.name(), builder.textNode(eventName));
        }

        private static final Map<Class<?>, Function<Object, JsonNode>> TO_JSON =
                Map.of(
                        String.class, v -> JsonNodeFactory.instance.textNode(v.toString()),
                        Integer.class, v -> JsonNodeFactory.instance.numberNode((Integer) v)
                );

        public Builder attribute(String name, JsonNode value) {
            JsonNode attributes = builder.get(Model.JsonDomainEventModel.attributes.name());
            if (attributes == null) {
                attributes = builder.objectNode();
                builder.set(Model.JsonDomainEventModel.attributes.name(), attributes);
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

            ObjectNode metadata = (ObjectNode) builder.get(Model.JsonDomainEventModel.metadata.name());
            if (metadata == null) {
                metadata = builder.objectNode();
                builder.set(Model.JsonDomainEventModel.metadata.name(), metadata);
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


        public JsonDomainEvent build() {
            return new JsonDomainEvent(builder);
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
        ObjectNode metadata = (ObjectNode) json.get(Model.JsonDomainEventModel.metadata.name());
        if (metadata == null)
            return Optional.empty();
        else
            return Optional.of(new Metadata(metadata));
    }
}
