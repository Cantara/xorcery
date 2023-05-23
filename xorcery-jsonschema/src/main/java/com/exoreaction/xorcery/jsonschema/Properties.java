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
package com.exoreaction.xorcery.jsonschema;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author rickardoberg
 */
public record Properties(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder) {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder property(String name, JsonSchema value) {
            builder.set(name, value.json());
            return this;
        }

        public Properties build() {
            return new Properties(builder);
        }
    }

    public Optional<JsonSchema> getPropertyByName(String name) {
        return Optional.ofNullable(object().get(name))
                .map(ObjectNode.class::cast)
                .map(JsonSchema::new);
    }

    public Map<String, JsonSchema> getProperties() {
        Function<JsonNode, ObjectNode> cast = ObjectNode.class::cast;
        return JsonElement.toMap(json, cast.andThen(JsonSchema::new));
    }
}
