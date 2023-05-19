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
package com.exoreaction.xorcery.jsonapi.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * @author rickardoberg
 */
public record Relationships(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder(Relationships relationships) {
            this(relationships.object().deepCopy());
        }

        public Builder relationship(Enum<?> name, Relationship relationship) {
            return relationship(name.name(), relationship);
        }

        public Builder relationship(String name, Relationship relationship) {
            builder.set(name, relationship.json());
            return this;
        }

        public Builder relationship(String name, Relationship.Builder relationship) {
            return relationship(name, relationship.build());
        }

        public Relationships build() {
            return new Relationships(builder);
        }
    }

    public boolean isEmpty()
    {
        return object().isEmpty();
    }

    public List<Relationship> getRelationshipList() {

        return JsonElement.getValuesAs(object(), Relationship::new);
    }

    public Map<String, Relationship> getRelationships() {
        Map<String, Relationship> rels = new HashMap<>(object().size());
        Iterator<Map.Entry<String, JsonNode>> fields = object().fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> next = fields.next();
            rels.put(next.getKey(), new Relationship((ObjectNode)next.getValue()));
        }
        return rels;
    }

    public Optional<Relationship> getRelationship(String name) {
        return Optional.ofNullable(object().get(name)).map(v -> new Relationship((ObjectNode) v));
    }

    public Optional<Relationship> getRelationship(Enum<?> name) {
        return getRelationship(name.name());
    }
}
