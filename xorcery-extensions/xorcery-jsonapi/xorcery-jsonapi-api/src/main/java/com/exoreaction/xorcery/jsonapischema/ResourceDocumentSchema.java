/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.jsonapischema;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.hyperschema.HyperSchema;
import com.exoreaction.xorcery.jsonapi.JsonApiRels;
import com.exoreaction.xorcery.jsonschema.Definitions;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.Properties;
import com.exoreaction.xorcery.jsonschema.Types;

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
                          Properties.Builder properties)
            implements With<Builder> {
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
    }
}
