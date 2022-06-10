package com.exoreaction.reactiveservices.jsonapi.schema;

import com.exoreaction.reactiveservices.hyperschema.model.HyperSchema;
import com.exoreaction.reactiveservices.hyperschema.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels;
import com.exoreaction.reactiveservices.jsonschema.model.Definitions;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import com.exoreaction.reactiveservices.jsonschema.model.Properties;
import com.exoreaction.reactiveservices.jsonschema.model.Types;
import com.exoreaction.util.With;

import java.util.ArrayList;
import java.util.List;

public record ResourceDocumentSchema(HyperSchema schema) {

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

    public record Builder(HyperSchema.Builder builder,
                          Definitions.Builder definitions,
                          Properties.Builder properties,
                          Links.Builder links)
    implements With<Builder>
    {
        public Builder() {
            this(new HyperSchema.Builder(new JsonSchema.Builder()),
                    new Definitions.Builder(),
                    new Properties.Builder(),
                    new Links.Builder());
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

        public Builder included(ResourceObjectSchema... resourceObjectSchemas)
        {
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
            builder.links(links.build())
                    .builder()
                    .definitions(definitions.build())
                    .properties(properties.build());

            return new ResourceDocumentSchema(builder.build());
        }
    }
}
