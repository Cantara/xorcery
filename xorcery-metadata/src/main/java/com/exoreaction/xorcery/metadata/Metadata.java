package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;


public final class Metadata
        implements JsonElement {
    private final ObjectNode json;


    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

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
            this.builder.setAll(metadata.json);
            return this;
        }

        public Metadata build() {
            return new Metadata(builder);
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

    @JsonCreator(mode = DELEGATING)
    public Metadata(ObjectNode json) {
        this.json = json;
    }

    @JsonValue
    public ObjectNode metadata() {
        return json;
    }

    public Builder toBuilder() {
        return new Builder(json);
    }

    public Metadata copy() {
        return new Metadata(json.deepCopy());
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Metadata) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Metadata[" +
               "json=" + json + ']';
    }

}
