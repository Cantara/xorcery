package com.exoreaction.xorcery.hyperschema.model;

import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Objects;
import java.util.Optional;

import static com.exoreaction.xorcery.jsonschema.model.JsonSchema.HYPER_SCHEMA_DRAFT_7;

public final class HyperSchema {
    private final JsonSchema schema;

    public HyperSchema(JsonSchema schema) {
        this.schema = schema;
    }

    public static final class Builder {
        private final JsonSchema.Builder builder;

        public Builder(JsonSchema.Builder builder) {
            this.builder = builder;
            builder.schema(HYPER_SCHEMA_DRAFT_7);
        }

        // Hyper Schema
        public Builder links(Links value) {
            builder.builder().set("links", value.json());
            return this;
        }

        public HyperSchema build() {
            return new HyperSchema(builder.build());
        }

        public JsonSchema.Builder builder() {
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

    public Links getLinks() {
        return Optional.ofNullable(schema.object().get("links"))
                .map(ArrayNode.class::cast)
                .map(Links::new)
                .orElseGet(() -> new Links(schema.json().arrayNode()));
    }

    public JsonSchema schema() {
        return schema;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HyperSchema) obj;
        return Objects.equals(this.schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema);
    }

    @Override
    public String toString() {
        return "HyperSchema[" +
               "schema=" + schema + ']';
    }

}
