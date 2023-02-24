package com.exoreaction.xorcery.jsonapischema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.hyperschema.model.HyperSchema;
import com.exoreaction.xorcery.jsonapi.model.JsonApiRels;
import com.exoreaction.xorcery.jsonschema.model.Definitions;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.exoreaction.xorcery.jsonschema.model.Properties;
import com.exoreaction.xorcery.jsonschema.model.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ResourceDocumentSchema {

    private static final String HASH_DEF = "#/definitions/";

    private static JsonSchema resourceObjectIdentifier = new JsonSchema.Builder()
            .type(Types.Object)
            .additionalProperties(false)
            .required("data")
            .properties(new Properties.Builder().property("data", new JsonSchema.Builder()
                    .type(Types.Object)
                    .additionalProperties(false)
                    .required("id", "type")
                    .properties(new Properties.Builder()
                            .property("id", new JsonSchema.Builder().type(Types.String).build())
                            .property("type", new JsonSchema.Builder().type(Types.String).build())
                            .build())
                    .build()).build())
            .build();

    public static JsonSchema resourceObjectIdentifierRef = new JsonSchema.Builder()
            .ref("#/definitions/resourceobjectidentifier")
            .build();

    private static JsonSchema relationshipRelatedLinks = new JsonSchema.Builder()
            .type(Types.Object)
            .additionalProperties(false)
            .properties(new Properties.Builder()
                    .property("links", new JsonSchema.Builder()
                            .additionalProperties(false)
                            .properties(new Properties.Builder()
                                    .property(JsonApiRels.related, new JsonSchema.Builder().type(Types.String).build())
                                    .build())
                            .build())
                    .build())
            .build();

    public static JsonSchema relationshipRelatedLinksRef = new JsonSchema.Builder()
            .ref("#/definitions/relationshiprelatedlinks")
            .build();
    private final HyperSchema schema;

    public ResourceDocumentSchema(HyperSchema schema) {
        this.schema = schema;
    }

    public HyperSchema schema() {
        return schema;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ResourceDocumentSchema) obj;
        return Objects.equals(this.schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schema);
    }

    @Override
    public String toString() {
        return "ResourceDocumentSchema[" +
               "schema=" + schema + ']';
    }


    public static final class Builder
                implements With<Builder> {
        private final HyperSchema.Builder builder;
        private final Definitions.Builder definitions;
        private final Properties.Builder properties;

        public Builder(HyperSchema.Builder builder,
                       Definitions.Builder definitions,
                       Properties.Builder properties) {
            this.builder = builder;
            this.definitions = definitions;
            this.properties = properties;
        }

        public Builder() {
                this(new HyperSchema.Builder(new JsonSchema.Builder()),
                        new Definitions.Builder(),
                        new Properties.Builder());
                builder.builder()
                        .type(Types.Object);

                definitions.definition("resourceobjectidentifier", resourceObjectIdentifier);
                definitions.definition("relationshiprelatedlinks", relationshipRelatedLinks);
            }

            public Builder resource(ResourceObjectSchema resourceObjectSchema) {
                resourceObjectSchema.schema().schema().getProperties()
                        .getPropertyByName("type")
                        .flatMap(JsonSchema::getConstant).ifPresent(type ->
                        {
                            definitions.definition(type, resourceObjectSchema.schema().schema());
                            properties.property("data", new JsonSchema.Builder().ref(HASH_DEF + type).build());
                        });
                return this;
            }

            public Builder resources(ResourceObjectSchema... resourceObjectSchemas) {
                List<JsonSchema> refs = new ArrayList<>();
                for (ResourceObjectSchema resourceObjectSchema : resourceObjectSchemas) {
                    resourceObjectSchema.schema().schema().getProperties()
                            .getPropertyByName("type")
                            .flatMap(JsonSchema::getConstant).ifPresent(type ->
                            {
                                definitions.definition(type, resourceObjectSchema.schema().schema());
                                refs.add(new JsonSchema.Builder().ref(HASH_DEF + type).build());
                            });
                }

                properties.property("data", new JsonSchema.Builder()
                        .type(Types.Array)
                        .items(refs)
                        .build());

                return this;
            }

            public Builder included(ResourceObjectSchema... resourceObjectSchemas) {
                List<JsonSchema> refs = new ArrayList<>();
                for (ResourceObjectSchema resourceObjectSchema : resourceObjectSchemas) {
                    resourceObjectSchema.schema().schema().getProperties()
                            .getPropertyByName("type")
                            .flatMap(JsonSchema::getConstant).ifPresent(type ->
                            {
                                definitions.definition(type, resourceObjectSchema.schema().schema());
                                refs.add(new JsonSchema.Builder().ref(HASH_DEF + type).build());
                            });
                }

                properties.property("included", new JsonSchema.Builder()
                        .type(Types.Array)
                        .items(refs)
                        .build());

                return this;
            }

            public ResourceDocumentSchema build() {
                builder.builder()
                        .definitions(definitions.build())
                        .properties(properties.build());

                return new ResourceDocumentSchema(builder.build());
            }

        public HyperSchema.Builder builder() {
            return builder;
        }

        public Definitions.Builder definitions() {
            return definitions;
        }

        public Properties.Builder properties() {
            return properties;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder) &&
                   Objects.equals(this.definitions, that.definitions) &&
                   Objects.equals(this.properties, that.properties);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder, definitions, properties);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ", " +
                   "definitions=" + definitions + ", " +
                   "properties=" + properties + ']';
        }

        }
}
