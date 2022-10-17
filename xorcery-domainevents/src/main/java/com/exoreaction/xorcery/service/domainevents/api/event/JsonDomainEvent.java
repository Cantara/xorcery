package com.exoreaction.xorcery.service.domainevents.api.event;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.domainevents.api.entity.DomainEvent;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.function.Function;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;

public record JsonDomainEvent(ObjectNode json)
        implements JsonElement, DomainEvent {

    public static Builder event(String eventName) {
        return new JsonDomainEvent.Builder(eventName);
    }

    public record Builder(ObjectNode builder) {

        public Builder(String eventName) {
            this(JsonNodeFactory.instance.objectNode());
            builder.set("event", builder.textNode(eventName));
        }

        public StateBuilder created(String type, String id) {
            builder.set("created", builder.objectNode()
                    .<ObjectNode>set("type", builder.textNode(type))
                    .set("id", builder.textNode(id)));

            return new StateBuilder(builder);
        }

        public StateBuilder updated(String type, String id) {
            builder.set("updated", builder.objectNode()
                    .<ObjectNode>set("type", builder.textNode(type))
                    .set("id", builder.textNode(id)));

            return new StateBuilder(builder);
        }

        public JsonDomainEvent deleted(String type, String id) {
            builder.set("deleted", builder.objectNode()
                    .<ObjectNode>set("type", builder.textNode(type))
                    .set("id", builder.textNode(id)));

            return new JsonDomainEvent(builder);
        }
    }

    public record StateBuilder(ObjectNode builder) {
        private static final Map<Class<?>, Function<Object, JsonNode>> TO_JSON =
                Map.of(
                        String.class, v -> JsonNodeFactory.instance.textNode(v.toString()),
                        Integer.class, v -> JsonNodeFactory.instance.numberNode((Integer) v)
                );

        public StateBuilder attribute(String name, JsonNode value) {
            JsonNode attributes = builder.get("attributes");
            if (attributes == null) {
                attributes = builder.objectNode();
                builder.set("attributes", attributes);
            }

            ObjectNode objectNode = (ObjectNode) attributes;
            objectNode.set(name, value);

            return this;
        }

        public StateBuilder attribute(String name, Object value) {
            if (value == null) {
                attribute(name, NullNode.getInstance());
            } else {
                attribute(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public StateBuilder addedRelationships(String relationship, String type, String id) {
            JsonNode relationships = builder.get("addedrelationships");
            if (relationships == null) {
                relationships = builder.arrayNode();
                builder.set("addedrelationships", relationships);
            }

            ArrayNode arrayNode = (ArrayNode) relationships;
            JsonNode relationshipNode = arrayNode.objectNode()
                    .<ObjectNode>set("type", arrayNode.textNode(type))
                    .<ObjectNode>set("id", arrayNode.textNode(id))
                    .set("relationship", arrayNode.textNode(relationship));
            arrayNode.add(relationshipNode);

            return this;
        }

        public StateBuilder removedRelationship(String relationship, String type, String id) {
            JsonNode relationships = builder.get("removedrelationships");
            if (relationships == null) {
                relationships = builder.arrayNode();
                builder.set("removedrelationships", relationships);
            }

            ArrayNode arrayNode = (ArrayNode) relationships;
            JsonNode relationshipNode = arrayNode.objectNode()
                    .<ObjectNode>set("type", arrayNode.textNode(type))
                    .<ObjectNode>set("id", arrayNode.textNode(id))
                    .set("relationship", arrayNode.textNode(relationship));
            arrayNode.add(relationshipNode);

            return this;
        }

        public JsonDomainEvent build() {
            return new JsonDomainEvent(builder);
        }
    }

    @JsonCreator(mode = DELEGATING)
    public JsonDomainEvent {
    }

    @JsonValue
    public ObjectNode event() {
        return json;
    }
}
