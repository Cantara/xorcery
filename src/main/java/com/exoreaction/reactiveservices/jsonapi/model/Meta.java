package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.builders.With;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */
public record Meta(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode json)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder meta(String name, JsonNode value) {
            json.set(name, value);
            return this;
        }

        public Builder meta(String name, long value) {
            json.set(name, json.numberNode(value));
            return this;
        }

        public Meta build() {
            return new Meta(json);
        }
    }

    public ObjectNode getMeta() {
        return object();
    }
}
