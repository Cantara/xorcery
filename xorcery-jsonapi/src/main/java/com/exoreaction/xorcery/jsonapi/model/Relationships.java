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
public final class Relationships
        implements JsonElement {
    private final ObjectNode json;

    /**
     *
     */
    public Relationships(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder
            implements With<Builder> {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

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

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
        }

    }

    public boolean isEmpty() {
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
            rels.put(next.getKey(), new Relationship((ObjectNode) next.getValue()));
        }
        return rels;
    }

    public Optional<Relationship> getRelationship(String name) {
        return Optional.ofNullable(object().get(name)).map(v -> new Relationship((ObjectNode) v));
    }

    public Optional<Relationship> getRelationship(Enum<?> name) {
        return getRelationship(name.name());
    }

    @Override
    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Relationships) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Relationships[" +
               "json=" + json + ']';
    }

}
