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
package com.exoreaction.xorcery.domainevents.api;

import com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel.*;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;

public record JsonDomainEvent(ObjectNode json)
        implements JsonElement, DomainEvent {

    public static Builder event(String eventName) {
        return new JsonDomainEvent.Builder(eventName);
    }

    public record Builder(ObjectNode builder) {

        public Builder(String eventName) {
            this(JsonNodeFactory.instance.objectNode());
            builder.set(event.name(), builder.textNode(eventName));
        }

        public StateBuilder created(String entityType, String entityId) {
            builder.set(created.name(), builder.objectNode()
                    .<ObjectNode>set(type.name(), builder.textNode(entityType))
                    .set(id.name(), builder.textNode(entityId)));

            return new StateBuilder(builder);
        }

        public StateBuilder created(Enum<?> type, String id) {
            return created(type.name(), id);
        }

        public StateBuilder updated(String entityType, String entityId) {
            builder.set(updated.name(), builder.objectNode()
                    .<ObjectNode>set(type.name(), builder.textNode(entityType))
                    .set(id.name(), builder.textNode(entityId)));

            return new StateBuilder(builder);
        }

        public StateBuilder updated(Enum<?> type, String id) {
            return updated(type.name(), id);
        }

        public JsonDomainEvent deleted(String entityType, String entityId) {
            builder.set(deleted.name(), builder.objectNode()
                    .<ObjectNode>set(type.name(), builder.textNode(entityType))
                    .set(id.name(), builder.textNode(entityId)));

            return new JsonDomainEvent(builder);
        }

        public JsonDomainEvent deleted(Enum<?> type, String id) {
            return deleted(type.name(), id);
        }
    }

    public record StateBuilder(ObjectNode builder) {
        private static final Map<Class<?>, Function<Object, JsonNode>> TO_JSON =
                Map.of(
                        String.class, v -> JsonNodeFactory.instance.textNode(v.toString()),
                        Integer.class, v -> JsonNodeFactory.instance.numberNode((Integer) v)
                );

        public StateBuilder attribute(String name, JsonNode value) {
            JsonNode attributes = builder.get(JsonDomainEventModel.attributes.name());
            if (attributes == null) {
                attributes = builder.objectNode();
                builder.set(JsonDomainEventModel.attributes.name(), attributes);
            }

            ObjectNode objectNode = (ObjectNode) attributes;
            objectNode.set(name, value);

            return this;
        }

        public StateBuilder attribute(Enum<?> name, JsonNode value) {
            return attribute(name.name(), value);
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

        public StateBuilder attribute(Enum<?> name, Object value) {
            return attribute(name.name(), value);
        }

        public StateBuilder updatedRelationship(String relationship, String entityType, String entityId, ObjectNode attributes) {

            if (entityId == null)
                return this;

            JsonNode relationships = builder.get(updatedrelationships.name());
            if (relationships == null) {
                relationships = builder.arrayNode();
                builder.set(updatedrelationships.name(), relationships);
            }

            ArrayNode arrayNode = (ArrayNode) relationships;
            ObjectNode relationshipNode = arrayNode.objectNode()
                    .<ObjectNode>set(type.name(), arrayNode.textNode(entityType))
                    .<ObjectNode>set(id.name(), arrayNode.textNode(entityId))
                    .set(JsonDomainEventModel.relationship.name(), arrayNode.textNode(relationship));
            if (attributes != null) {
                relationshipNode.set(JsonDomainEventModel.attributes.name(), attributes);
            }
            arrayNode.add(relationshipNode);

            return this;
        }

        public StateBuilder updatedRelationship(Enum<?> relationship, Enum<?> type, String id, ObjectNode attributes) {
            return updatedRelationship(relationship.name(), type.name(), id, attributes);
        }

        public StateBuilder updatedRelationship(String relationship, String type, String id) {
            return updatedRelationship(relationship, type, id, null);
        }

        public StateBuilder updatedRelationship(Enum<?> relationship, Enum<?> type, String id) {
            return updatedRelationship(relationship.name(), type.name(), id, null);
        }

        public StateBuilder addedRelationship(String relationship, String entityType, String entityId) {

            if (entityId == null)
                return this;

            JsonNode relationships = builder.get(addedrelationships.name());
            if (relationships == null) {
                relationships = builder.arrayNode();
                builder.set(addedrelationships.name(), relationships);
            }

            ArrayNode arrayNode = (ArrayNode) relationships;
            JsonNode relationshipNode = arrayNode.objectNode()
                    .<ObjectNode>set(type.name(), arrayNode.textNode(entityType))
                    .<ObjectNode>set(id.name(), arrayNode.textNode(entityId))
                    .set(JsonDomainEventModel.relationship.name(), arrayNode.textNode(relationship));
            arrayNode.add(relationshipNode);

            return this;
        }

        public StateBuilder addedRelationship(Enum<?> relationship, Enum<?> type, String id) {
            return addedRelationship(relationship.name(), type.name(), id);
        }

        public StateBuilder removedRelationship(String relationship, String entityType, String entityId) {

            JsonNode relationships = builder.get(removedrelationships.name());
            if (relationships == null) {
                relationships = builder.arrayNode();
                builder.set(removedrelationships.name(), relationships);
            }

            ArrayNode arrayNode = (ArrayNode) relationships;
            JsonNode relationshipNode = arrayNode.objectNode()
                    .<ObjectNode>set(type.name(), arrayNode.textNode(entityType))
                    .<ObjectNode>set(id.name(), entityId == null ? arrayNode.nullNode() : arrayNode.textNode(entityId))
                    .set(JsonDomainEventModel.relationship.name(), arrayNode.textNode(relationship));
            arrayNode.add(relationshipNode);

            return this;
        }

        public StateBuilder removedRelationship(Enum<?> relationship, Enum<?> type, String id) {
            return removedRelationship(relationship.name(), type.name(), id);
        }

        public StateBuilder addMetadata(String key, JsonNode value) {

            ObjectNode metadata = (ObjectNode) builder.get(JsonDomainEventModel.metadata.name());
            if (metadata == null) {
                metadata = builder.objectNode();
                builder.set(JsonDomainEventModel.metadata.name(), metadata);
            }

            metadata.set(key, value);

            return this;
        }

        public StateBuilder addMetadata(String name, Object value) {
            if (value == null) {
                addMetadata(name, NullNode.getInstance());
            } else {
                addMetadata(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public StateBuilder addMetadata(Enum<?> name, Object value) {
            return addMetadata(name.name(), value);
        }

        public StateBuilder addMetadata(String name, Optional<?> optional) {
            if (optional.isEmpty()) {
                return this;
            }
            var value = optional.get();
            return value instanceof JsonNode ?
                    addMetadata(name, (JsonNode) value) :
                    addMetadata(name, value);
        }

        public StateBuilder addMetadata(Enum<?> name, Optional<?> optional) {
            return addMetadata(name.name(), optional);
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

    public String getName() {
        return json.path("event").asText();
    }

    public boolean isCreatedOrUpdated() {
        return json.has(created.name()) || json.has(updated.name());
    }

    public boolean isDeleted() {
        return json.has(deleted.name());
    }

    public JsonEntity getCreated() {
        ObjectNode created = (ObjectNode) json.get(JsonDomainEventModel.created.name());
        return created == null ? null : new JsonEntity(created);
    }

    public JsonEntity getUpdated() {
        ObjectNode updated = (ObjectNode) json.get(JsonDomainEventModel.updated.name());
        return updated == null ? null : new JsonEntity(updated);
    }

    public JsonEntity getDeleted() {
        ObjectNode deleted = (ObjectNode) json.get(JsonDomainEventModel.deleted.name());
        return deleted == null ? null : new JsonEntity(deleted);
    }

    public JsonEntity getEntity() {
        JsonEntity entity = getUpdated();
        if (entity != null)
            return entity;
        entity = getCreated();
        if (entity != null)
            return entity;
        return getDeleted();
    }

    public Map<String, JsonNode> getAttributes() {
        ObjectNode attrs = (ObjectNode) json.get(attributes.name());
        if (attrs == null)
            return Collections.emptyMap();
        else
            return JsonElement.asMap(attrs);
    }

    public List<JsonRelationship> getUpdatedRelationships() {
        return getListAs(updatedrelationships.name(), json -> new JsonRelationship((ObjectNode) json))
                .orElse(Collections.emptyList());
    }

    public List<JsonRelationship> getAddedRelationships() {
        return getListAs(addedrelationships.name(), json -> new JsonRelationship((ObjectNode) json))
                .orElse(Collections.emptyList());
    }

    public List<JsonRelationship> getRemovedRelationships() {
        return getListAs(removedrelationships.name(), json -> new JsonRelationship((ObjectNode) json))
                .orElse(Collections.emptyList());
    }

    public Optional<Metadata> getMetadata() {
        ObjectNode metadata = (ObjectNode) json.get(JsonDomainEventModel.metadata.name());
        if (metadata == null)
            return Optional.empty();
        else
            return Optional.of(new Metadata(metadata));
    }

    public record JsonEntity(ObjectNode json)
            implements JsonElement {
        public String getType() {
            return getString(type.name()).orElse(null);
        }

        public String getId() {
            return getString(id.name()).orElse(null);
        }
    }

    public record JsonRelationship(ObjectNode json)
            implements JsonElement {
        public JsonEntity getEntity() {
            return new JsonEntity(json);
        }

        public String getRelationship() {
            return getString(relationship.name()).orElseThrow();
        }
    }
}
