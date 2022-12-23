package com.exoreaction.xorcery.service.domainevents.neo4jprojections;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.domainevents.api.event.JsonDomainEvent;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
