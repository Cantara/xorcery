package com.exoreaction.xorcery.jsonschema.model;

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
public final class Properties
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Properties(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder property(String name, JsonSchema value) {
            builder.set(name, value.json());
            return this;
        }

        public Properties build() {
            return new Properties(builder);
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

    public Optional<JsonSchema> getPropertyByName(String name) {
        return Optional.ofNullable(object().get(name))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Map<String, JsonSchema> getProperties() {
        Function<JsonNode, ObjectNode> cast = ObjectNode.class::cast;
        return JsonElement.toMap(json, cast.andThen(JsonSchema::new));
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Properties) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Properties[" +
               "json=" + json + ']';
    }

}
