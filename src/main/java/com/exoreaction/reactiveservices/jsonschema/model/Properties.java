package com.exoreaction.reactiveservices.jsonschema.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.json.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author rickardoberg
 */
public record Properties(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder) {
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
    }

    public Optional<JsonSchema> getPropertyByName(String name) {
        return Optional.ofNullable(object().get(name))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Map<String, JsonSchema> getProperties() {
        Function<JsonNode, ObjectNode> cast = ObjectNode.class::cast;
        return JsonNodes.toMap(json, cast.andThen(JsonSchema::new));
    }
}
