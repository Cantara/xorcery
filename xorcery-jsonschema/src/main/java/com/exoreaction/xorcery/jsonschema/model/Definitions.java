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
package com.exoreaction.xorcery.jsonschema.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author rickardoberg
 */
public record Definitions(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder definition(String name, JsonSchema value) {
            builder.set(name, value.json());
            return this;
        }

        public Definitions build() {
            return new Definitions(builder);
        }
    }

    public Optional<JsonSchema> getDefinition(String name) {
        return Optional.ofNullable(object().get(name))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Map<String, JsonSchema> getDefinitions() {
        Function<JsonNode, ObjectNode> cast = ObjectNode.class::cast;
        return JsonElement.toMap(json, cast.andThen(JsonSchema::new));
    }

    public JsonSchema resolve(JsonSchema schema) {
        return schema.getRef()
                .map(ref -> ref.substring(ref.lastIndexOf('/')))
                .flatMap(this::getDefinition)
                .orElse(schema);
    }
}
