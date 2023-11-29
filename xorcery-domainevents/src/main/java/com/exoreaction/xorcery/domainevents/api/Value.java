package com.exoreaction.xorcery.domainevents.api;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.util.*;
import java.util.function.Consumer;

import static com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel.*;
import static com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel.removedrelationships;

public final class Value
        implements JsonElement {
    public static Value value() {
        return new Value(JsonNodeFactory.instance.objectNode());
    }

    public final ObjectNode builder;

    public Value(ObjectNode instance) {
        builder = instance;
    }

    public Value attribute(String name, JsonNode value) {
        JsonNode attributes = builder.get("attributes");
        if (attributes == null) {
            attributes = builder.objectNode();
            builder.set("attributes", attributes);
        }

        ObjectNode objectNode = (ObjectNode) attributes;
        objectNode.set(name, value);

        return this;
    }

    public Value attribute(Enum<?> name, JsonNode value) {
        return attribute(name.name(), value);
    }

    public Value attribute(String name, Object value) {
        if (value == null) {
            attribute(name, NullNode.getInstance());
        } else {
            attribute(name, JsonDomainEvent.TO_JSON
                    .getOrDefault(value.getClass(), (v) -> JsonDomainEvent.mapper.canSerialize(value.getClass()) ? JsonDomainEvent.mapper.valueToTree(value) : JsonNodeFactory.instance.textNode(v.toString()))
                    .apply(value));
        }

        return this;
    }

    public Value attribute(Enum<?> name, Object value) {
        return attribute(name.name(), value);
    }

    public Value relationship(String relationship, String type, String id) {

        if (id == null)
            return this;

        JsonNode relationships = builder.get("relationships");
        if (relationships == null) {
            relationships = builder.objectNode();
            builder.set("relationships", relationships);
        }

        ObjectNode relationshipsNode = (ObjectNode) relationships;
        ObjectNode relationshipNode = relationshipsNode.objectNode()
                .<ObjectNode>set("type", relationshipsNode.textNode(type))
                .set("id", relationshipsNode.textNode(id));
        relationshipsNode.set(relationship, relationshipNode);

        return this;
    }

    public Value relationship(Enum<?> relationship, Enum<?> type, String id) {
        return relationship(relationship.name(), type.name(), id);
    }

    public Value relationship(String relationship, String type, Collection<String> ids) {

        if (ids == null)
            return this;

        JsonNode relationships = builder.get("relationships");
        if (relationships == null) {
            relationships = builder.objectNode();
            builder.set("relationships", relationships);
        }

        ObjectNode relationshipsNode = (ObjectNode) relationships;
        ArrayNode relationshipList = relationshipsNode.arrayNode();
        for (String id : ids) {
            ObjectNode relationshipNode = relationshipsNode.objectNode()
                    .<ObjectNode>set("type", relationshipsNode.textNode(type))
                    .set("id", relationshipsNode.textNode(id));
            relationshipList.add(relationshipNode);
        }
        relationshipsNode.set(relationship, relationshipList);

        return this;
    }

    public Value relationship(Enum<?> relationship, Enum<?> type, Collection<String> ids) {
        return relationship(relationship.name(), type.name(), ids);
    }

    @SuppressWarnings("unchecked")
    public Value with(Consumer<Value>... consumers) {
        for (Consumer<Value> consumer : consumers) {
            consumer.accept(this);
        }
        return this;
    }

    @JsonValue
    public ObjectNode json() {
        return builder;
    }

    public Map<String, JsonNode> getAttributes() {
        ObjectNode attrs = (ObjectNode) builder.get(attributes.name());
        if (attrs == null)
            return Collections.emptyMap();
        else
            return JsonElement.asMap(attrs);
    }

    public Map<String, ValueRelationship> getRelationships() {
        return getJson(Model.ValueModel.relationships.name())
                .map(ObjectNode.class::cast)
                .map(on -> JsonElement.toMap(on, ValueRelationship::new))
                .orElse(Collections.emptyMap());
    }

    public record ValueRelationship(ContainerNode<?> json)
            implements JsonElement {

        public ValueRelationship(JsonNode json) {
            this((ContainerNode<?>) json);
        }

        public boolean isSingle() {
            return json instanceof ObjectNode;
        }

        public JsonDomainEvent.JsonRelationship getRelationship() {
            if (json instanceof ObjectNode objectNode)
                return new JsonDomainEvent.JsonRelationship(objectNode);
            else
                return null;
        }

        public List<JsonDomainEvent.JsonRelationship> getRelationships() {
            if (json instanceof ArrayNode arrayNode) {
                List<JsonDomainEvent.JsonRelationship> relationships = new ArrayList<>(arrayNode.size());
                for (JsonNode jsonNode : arrayNode) {
                    relationships.add(new JsonDomainEvent.JsonRelationship((ObjectNode) jsonNode));
                }
                return relationships;
            } else
                return Collections.emptyList();
        }
    }
}
