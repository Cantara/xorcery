package com.exoreaction.xorcery.jsonapischema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.hyperschema.model.HyperSchema;
import com.exoreaction.xorcery.hyperschema.model.Link;
import com.exoreaction.xorcery.hyperschema.model.Links;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.exoreaction.xorcery.jsonschema.model.Properties;
import com.exoreaction.xorcery.jsonschema.model.Types;

import java.util.Objects;
import java.util.function.Consumer;

public final class ResourceObjectSchema {
    private final HyperSchema schema;

    public ResourceObjectSchema(HyperSchema schema) {
        this.schema = schema;
    }

    public HyperSchema schema() {
        return schema;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceObjectSchema) obj;
        return Objects.equals(this.schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema);
    }

    @Override
    public String toString() {
        return "ResourceObjectSchema[" +
               "schema=" + schema + ']';
    }


    public static final class Builder
            implements With<Builder> {
        private final HyperSchema.Builder builder;
        private final Properties.Builder properties;
        private final Properties.Builder attributes;
        private final Properties.Builder relationships;
        private final Links.Builder links;

        public Builder(HyperSchema.Builder builder,
                       Properties.Builder properties,
                       Properties.Builder attributes,
                       Properties.Builder relationships,
                       Links.Builder links) {
            this.builder = builder;
            this.properties = properties;
            this.attributes = attributes;
            this.relationships = relationships;
            this.links = links;
        }

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

        public HyperSchema.Builder builder() {
            return builder;
        }

        public Properties.Builder properties() {
            return properties;
        }

        public Properties.Builder attributes() {
            return attributes;
        }

        public Properties.Builder relationships() {
            return relationships;
        }

        public Links.Builder links() {
            return links;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder) &&
                   Objects.equals(this.properties, that.properties) &&
                   Objects.equals(this.attributes, that.attributes) &&
                   Objects.equals(this.relationships, that.relationships) &&
                   Objects.equals(this.links, that.links);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder, properties, attributes, relationships, links);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ", " +
                   "properties=" + properties + ", " +
                   "attributes=" + attributes + ", " +
                   "relationships=" + relationships + ", " +
                   "links=" + links + ']';
        }

    }
}
