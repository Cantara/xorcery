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
package dev.xorcery.jsonapischema;

import dev.xorcery.builders.With;
import dev.xorcery.hyperschema.HyperSchema;
import dev.xorcery.hyperschema.Link;
import dev.xorcery.hyperschema.Links;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.Properties;
import dev.xorcery.jsonschema.Types;

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
