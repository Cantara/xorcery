package com.exoreaction.reactiveservices.cqrs.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;


public record Metadata(ObjectNode metadata) {
    public record Builder(ObjectNode builder) {

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder add(String name, String value) {
            builder.set(name, builder.textNode(value));
            return this;
        }

        public Builder add(String name, long value) {
            builder.set(name, builder.numberNode(value));
            return this;
        }

        public Builder add(String name, ObjectNode value) {
            builder.set(name, value);
            return this;
        }

        public Builder add(Metadata metadata) {
            this.builder.setAll(metadata.metadata);
            return this;
        }

        public Metadata build() {
            return new Metadata(builder);
        }
    }
    @JsonCreator(mode = DELEGATING)
    public Metadata {
    }

    public boolean has(String name) {
        return metadata.has(name);
    }

    public Optional<String> getString(String name) {
        return Optional.ofNullable(metadata.path(name).textValue());
    }

    public Optional<Long> getLong(String name) {
        return Optional.ofNullable(metadata.get(name)).map(JsonNode::longValue);
    }

    public Optional<JsonNode> getJsonNode(String name) {
        return Optional.ofNullable(metadata.get(name));
    }

    public Optional<ObjectNode> getObjectNode(String name) {
        return getJsonNode(name).map(ObjectNode.class::cast);
    }

    @JsonValue
    public ObjectNode metadata()
    {
        return metadata;
    }

    public Builder toBuilder() {
        return new Builder(metadata);
    }
}
