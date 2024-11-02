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
package dev.xorcery.domainevents.neo4jprojections;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.json.JsonElement;
import dev.xorcery.neo4j.client.Cypher;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public record Neo4jJsonDomainEvent(JsonDomainEvent context) {

    public record Builder(ObjectNode builder) {

        public Builder label(String label)
        {
            JsonNode labels = builder.get("labels");
            if (labels == null) {
                labels = builder.arrayNode();
                builder.set("labels", labels);
            }

            ((ArrayNode) labels).add(label);

            return this;
        }

        public Neo4jJsonDomainEvent build() {
            return new Neo4jJsonDomainEvent(new JsonDomainEvent(builder));
        }
    }

    public Map<String, Object> neo4jAttributes()
    {
        ObjectNode attrs = (ObjectNode) context.json().get("attributes");
        if (attrs == null)
            return Collections.emptyMap();
        else
            return Cypher.toMap(attrs);
    }

    public List<String> labels()
    {
        ArrayNode labels = (ArrayNode)context.json().get("labels");
        return labels == null ? Collections.emptyList() : JsonElement.getValuesAs(labels, JsonNode::textValue);
    }
}
