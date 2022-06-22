package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.util.builders.With;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */
public record Meta(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder meta(String name, JsonNode value) {
            builder.set(name, value);
            return this;
        }

        public Builder meta(String name, long value) {
            builder.set(name, builder.numberNode(value));
            return this;
        }

        public Meta build() {
            return new Meta(builder);
        }
    }

    public ObjectNode getMeta() {
        return object();
    }
}
