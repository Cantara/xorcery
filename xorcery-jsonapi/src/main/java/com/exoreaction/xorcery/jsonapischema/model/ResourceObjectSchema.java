package com.exoreaction.xorcery.jsonapischema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.hyperschema.model.HyperSchema;
import com.exoreaction.xorcery.hyperschema.model.Link;
import com.exoreaction.xorcery.hyperschema.model.Links;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.exoreaction.xorcery.jsonschema.model.Properties;
import com.exoreaction.xorcery.jsonschema.model.Types;

import java.util.function.Consumer;

public record ResourceObjectSchema(HyperSchema schema) {

    public record Builder(HyperSchema.Builder builder,
                          Properties.Builder properties,
                          Properties.Builder attributes,
                          Properties.Builder relationships,
                          Links.Builder links)
            implements With<Builder> {
        public Builder() {
            this(new HyperSchema.Builder(new JsonSchema.Builder()),
                    new Properties.Builder(),
                    new Properties.Builder(),
                    new Properties.Builder(),
                    new Links.Builder()
            );
        }

        public Builder type(Enum<?> value) {
            properties.property("type", new JsonSchema.Builder()
                            .type(Types.String)
                            .constant(builder.builder().builder().textNode(value.name()))
                            .build())
                    .property("id", new JsonSchema.Builder()
                            .type(Types.String)
                            .build());
            return this;
        }

        public Builder attributes(Consumer<Properties.Builder> with) {
            with.accept(attributes);
            return this;
        }

        public Builder relationships(Consumer<Properties.Builder> with) {
            with.accept(relationships);
            return this;
        }

        public Builder link(String rel) {
            links.link(new Link.Builder()
                    .rel(rel)
                    .href("{+link_href}")
                    .templateRequired("link_href")
                    .templatePointer("link_href", "0/data/links/" + rel)
                    .build());

            return this;
        }


        public ResourceObjectSchema build() {
            builder()
                    .links(links.build())
                    .builder()
                    .properties(properties
                            .property("relationships", new JsonSchema.Builder()
                                    .type(Types.Object)
                                    .additionalProperties(false)
                                    .properties(relationships.build())
                                    .build())
                            .property("attributes", new JsonSchema.Builder()
                                    .type(Types.Object)
                                    .additionalProperties(false)
                                    .properties(attributes.build())
                                    .build())
                            .build());
            return new ResourceObjectSchema(builder.build());
        }
    }
}
