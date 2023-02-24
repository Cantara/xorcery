package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

/**
 * @author rickardoberg
 */

public final class Error
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Error(ObjectNode json) {
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

    public String getStatus() {
        return getString("status").orElse(null);
    }

    public Source getSource() {
        JsonNode node = object().path("source");
        return new Source(node instanceof ObjectNode ? (ObjectNode) node :
                JsonNodeFactory.instance.objectNode());
    }

    public String getTitle() {
        return getString("title").orElse("");
    }

    public String getDetail() {
        return getString("detail").orElse("");
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Error) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Error[" +
               "json=" + json + ']';
    }

}
