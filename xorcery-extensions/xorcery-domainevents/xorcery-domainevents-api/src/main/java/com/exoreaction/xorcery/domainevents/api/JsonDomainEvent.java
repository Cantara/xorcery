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

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.function.Function;

import static com.exoreaction.xorcery.domainevents.api.Model.JsonDomainEventModel.*;
import static com.exoreaction.xorcery.lang.Strings.capitalize;
import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;
import static java.util.Objects.requireNonNull;

/**
 * These events signify changes to state in a domain model
 */
public record JsonDomainEvent(ObjectNode json)
        implements JsonElement, DomainEvent {
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
    static final ObjectMapper mapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    public static Builder event(String eventName) {
        return new JsonDomainEvent.Builder(eventName);
    }

    public static Builder event(String eventNamePrefix, Enum<?> eventNameSuffix) {
        return event(eventNamePrefix + capitalize(eventNameSuffix.name()));
    }

    public record Builder(ObjectNode builder)
        implements With<Builder>
    {
        public Builder(String eventName) {
            this(JsonNodeFactory.instance.objectNode());
            requireNonNull(eventName);
            builder.set("@class", builder.textNode("com.exoreaction.xorcery.domainevents.api.JsonDomainEvent"));
            builder.set(event.name(), builder.textNode(eventName));
        }

        public StateBuilder created(String entityType, String entityId) {
            requireNonNull(type);
            requireNonNull(id);
            builder.set(created.name(), builder.objectNode()
                    .<ObjectNode>set(type.name(), builder.textNode(entityType))
                    .set(id.name(), builder.textNode(entityId)));

            return new StateBuilder(builder);
        }

        public StateBuilder created(Enum<?> type, String id) {
            return created(type.name(), id);
        }

        public StateBuilder updated(String entityType, String entityId) {
            requireNonNull(type);
            requireNonNull(id);
            builder.set(updated.name(), builder.objectNode()
                    .<ObjectNode>set(type.name(), builder.textNode(entityType))
                    .set(id.name(), builder.textNode(entityId)));

            return new StateBuilder(builder);
        }

        public StateBuilder updated(Enum<?> type, String id) {
            return updated(type.name(), id);
        }

        public JsonDomainEvent deleted(String entityType, String entityId) {
            requireNonNull(type);
            requireNonNull(id);
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
        public StateBuilder updatedAttribute(String name, JsonNode value) {
            requireNonNull(name);
            JsonNode attributes = builder.get(JsonDomainEventModel.attributes.name());
            if (attributes == null) {
                attributes = builder.objectNode();
                builder.set(JsonDomainEventModel.attributes.name(), attributes);
            }

            ObjectNode objectNode = (ObjectNode) attributes;
            objectNode.set(name, value);

            return this;
        }

        public StateBuilder updatedAttribute(Enum<?> name, JsonNode value) {
            return updatedAttribute(name.name(), value);
        }

        public StateBuilder updatedAttribute(String name, Object value) {
            if (value == null) {
                updatedAttribute(name, NullNode.getInstance());
            } else {
                updatedAttribute(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> mapper.canSerialize(value.getClass()) ? mapper.valueToTree(value) : JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public StateBuilder updatedAttribute(Enum<?> name, Object value) {
            return updatedAttribute(name.name(), value);
        }

        // Collection attributes

        /**
         * Add a value to a list attribute.
         *
         * @param name  of attribute
         * @param index to insert value at, or -1 to insert at the end
         * @param value of attribute
         * @return the builder
         */
        public StateBuilder addedAttribute(String name, int index, JsonNode value) {
            requireNonNull(name);

            if (value == null || value.isNull() || value.isMissingNode())
                return this;

            JsonNode attributes = builder.get("addedattributes");
            if (attributes == null) {
                attributes = builder.arrayNode();
                builder.set("addedattributes", attributes);
            }

            ArrayNode arrayNode = (ArrayNode) attributes;
            ObjectNode attributeNode = arrayNode.objectNode()
                    .<ObjectNode>set("name", arrayNode.textNode(name))
                    .set("value", value);
            if (index >= 0) {
                attributeNode.set("index", arrayNode.numberNode(index));
            }
            arrayNode.add(attributeNode);

            return this;
        }

        public StateBuilder addedAttribute(String name, int index, Object value) {
            if (value == null) {
                return this;
            } else {
                addedAttribute(name, index, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> mapper.canSerialize(value.getClass()) ? mapper.valueToTree(value) : JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public StateBuilder addedAttribute(Enum<?> name, int index, Object value) {
            return addedAttribute(name.name(), index, value);
        }

        public StateBuilder addedAttribute(String name, JsonNode value) {
            return addedAttribute(name, -1, value);
        }

        public StateBuilder addedAttribute(String name, Object value) {
            return addedAttribute(name, -1, value);
        }

        public StateBuilder addedAttribute(Enum<?> name, Object value) {
            return addedAttribute(name.name(), -1, value);
        }

        /**
         * Remove a value from a list attribute
         *
         * @param name  of attribute
         * @param value of attribute
         * @return the builder
         */
        public StateBuilder removedAttribute(String name, JsonNode value) {
            requireNonNull(name);

            if (value == null || value.isNull() || value.isMissingNode())
                return this;

            JsonNode attributes = builder.get("removedattributes");
            if (attributes == null) {
                attributes = builder.arrayNode();
                builder.set("removedattributes", attributes);
            }

            ArrayNode arrayNode = (ArrayNode) attributes;
            ObjectNode attributeNode = arrayNode.objectNode()
                    .<ObjectNode>set("name", arrayNode.textNode(name))
                    .set("value", value);
            arrayNode.add(attributeNode);

            return this;
        }

        public StateBuilder removedAttribute(String name, Object value) {
            if (value == null) {
                return this;
            } else {
                removedAttribute(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> mapper.canSerialize(value.getClass()) ? mapper.valueToTree(value) : JsonNodeFactory.instance.textNode(v.toString()))
                        .apply(value));
            }

            return this;
        }

        public StateBuilder removedAttribute(Enum<?> name, Object value) {
            return removedAttribute(name.name(), value);
        }

        // Relationships
        public StateBuilder updatedRelationship(String relationship, String entityType, String entityId, ObjectNode attributes) {
            requireNonNull(relationship);
            requireNonNull(type);

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
            requireNonNull(relationship);
            requireNonNull(type);

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
            requireNonNull(relationship);
            requireNonNull(type);

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
            requireNonNull(name);
            if (value == null) {
                addMetadata(name, NullNode.getInstance());
            } else {
                addMetadata(name, TO_JSON
                        .getOrDefault(value.getClass(), (v) -> mapper.canSerialize(value.getClass()) ? mapper.valueToTree(value) : JsonNodeFactory.instance.textNode(v.toString()))
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

    public boolean isCreated() {
        return json.has(created.name());
    }

    public boolean isUpdated() {
        return json.has(updated.name());
    }

    public boolean isDeleted() {
        return json.has(deleted.name());
    }

    public boolean isCreatedOrUpdated() {
        return isUpdated() || isCreated();
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

    public List<NameValue> getAddedAttributes() {
        ArrayNode attrs = (ArrayNode) json.get(addedattributes.name());
        if (attrs == null)
            return Collections.emptyList();
        else
        {
            List<NameValue> nameValueList = new ArrayList<>(attrs.size());
            for (JsonNode attr : attrs) {
                nameValueList.add(new NameValue(attr.path("name").textValue(), attr.get("value")));
            }
            return nameValueList;
        }
    }

    public List<NameValue> getRemovedAttributes() {
        ArrayNode attrs = (ArrayNode) json.get(removedattributes.name());
        if (attrs == null)
            return Collections.emptyList();
        else
        {
            List<NameValue> nameValueList = new ArrayList<>(attrs.size());
            for (JsonNode attr : attrs) {
                nameValueList.add(new NameValue(attr.path("name").textValue(), attr.get("value")));
            }
            return nameValueList;
        }
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

    public JsonDomainEvent cloneWithoutState()
    {
        ObjectNode cloned = json.deepCopy();
        cloned.remove(Model.JsonDomainEventModel.attributes.name());
        cloned.remove(Model.JsonDomainEventModel.addedattributes.name());
        cloned.remove(Model.JsonDomainEventModel.removedattributes.name());
        cloned.remove(Model.JsonDomainEventModel.updatedrelationships.name());
        cloned.remove(Model.JsonDomainEventModel.addedrelationships.name());
        cloned.remove(Model.JsonDomainEventModel.removedrelationships.name());
        return new JsonDomainEvent(cloned);
    }


    @Override
    public String toString() {
        return json.toPrettyString();
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
