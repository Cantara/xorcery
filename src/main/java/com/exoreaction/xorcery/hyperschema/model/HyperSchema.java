package com.exoreaction.xorcery.hyperschema.model;

import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Optional;

import static com.exoreaction.xorcery.jsonschema.model.JsonSchema.HYPER_SCHEMA_DRAFT_7;

public record HyperSchema(JsonSchema schema) {

    public record Builder(JsonSchema.Builder builder)
    {
        public Builder(JsonSchema.Builder builder)
        {
            this.builder = builder;
            builder.schema(HYPER_SCHEMA_DRAFT_7);
        }

        // Hyper Schema
        public Builder links(Links value) {
            builder.builder().set("links", value.json());
            return this;
        }

        public HyperSchema build()
        {
            return new HyperSchema(builder.build());
        }
    }

    public Links getLinks() {
        return Optional.ofNullable(schema.object().get("links"))
                .map(ArrayNode.class::cast)
                .map(Links::new)
                .orElseGet(() -> new Links(schema.json().arrayNode()));
    }
}
