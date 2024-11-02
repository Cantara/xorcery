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
package dev.xorcery.hyperschema;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dev.xorcery.jsonschema.JsonSchema;

import java.util.Optional;

import static dev.xorcery.jsonschema.JsonSchema.HYPER_SCHEMA_DRAFT_7;

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
