package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import com.exoreaction.util.json.JsonNodes;
import com.exoreaction.util.builders.With;
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

    public List<Relationship> getRelationshipList() {

        return JsonNodes.getValuesAs(object(), Relationship::new);
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
