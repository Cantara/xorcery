package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.With;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */
public record Source(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder pointer(String value) {
            builder.set("pointer", builder.textNode(value));
            return this;
        }

        public Builder parameter(String value) {
            builder.set("detail", builder.textNode(value));
            return this;
        }

        public Source build() {
            return new Source(builder);
        }
    }

    public String getPointer() {
        return getOptionalString("pointer").orElse(null);
    }

    public String getParameter() {
        return getOptionalString("parameter").orElse(null);
    }
}
