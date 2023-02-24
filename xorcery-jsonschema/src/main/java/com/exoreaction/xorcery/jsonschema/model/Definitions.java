package com.exoreaction.xorcery.jsonschema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author rickardoberg
 */
public final class Definitions
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Definitions(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder definition(String name, JsonSchema value) {
            builder.set(name, value.json());
            return this;
        }

        public Definitions build() {
            return new Definitions(builder);
        }

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
        }

    }

    public Optional<JsonSchema> getDefinition(String name) {
        return Optional.ofNullable(object().get(name))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Map<String, JsonSchema> getDefinitions() {
        Function<JsonNode, ObjectNode> cast = ObjectNode.class::cast;
        return JsonElement.toMap(json, cast.andThen(JsonSchema::new));
    }

    public JsonSchema resolve(JsonSchema schema) {
        return schema.getRef()
                .map(ref -> ref.substring(ref.lastIndexOf('/')))
                .flatMap(this::getDefinition)
                .orElse(schema);
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Definitions) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Definitions[" +
               "json=" + json + ']';
    }

}
