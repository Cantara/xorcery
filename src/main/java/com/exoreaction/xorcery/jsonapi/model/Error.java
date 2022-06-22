package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.util.builders.With;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */

public record Error(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder status(int value) {
            builder.set("status", builder.textNode(Integer.toString(value)));
            return this;
        }

        public Builder source(Source source) {
            builder.set("source", source.object());
            return this;
        }

        public Builder title(String value) {
            builder.set("title", builder.textNode(value));
            return this;
        }

        public Builder detail(String value) {
            builder.set("detail", builder.textNode(value));
            return this;
        }

        public Error build() {
            return new Error(builder);
        }
    }

    public String getStatus() {
        return getOptionalString("status").orElse(null);
    }

    public Source getSource() {
        return new Source(object().path("source") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public String getTitle() {
        return getString("title");
    }

    public String getDetail() {
        return getString("detail");
    }
}
