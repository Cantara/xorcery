package com.exoreaction.xorcery.jsonschema.model;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.util.builders.With;
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
public record Definitions(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<Builder> {
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
    }

    public Optional<JsonSchema> getDefinition(String name) {
        return Optional.ofNullable(object().get(name))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Map<String, JsonSchema> getDefinitions() {
        Function<JsonNode, ObjectNode> cast = ObjectNode.class::cast;
        return JsonNodes.toMap(json, cast.andThen(JsonSchema::new));
    }

    public JsonSchema resolve(JsonSchema schema) {
        return schema.getRef()
                .map(ref -> ref.substring(ref.lastIndexOf('/')))
                .flatMap(this::getDefinition)
                .orElse(schema);
    }
}
