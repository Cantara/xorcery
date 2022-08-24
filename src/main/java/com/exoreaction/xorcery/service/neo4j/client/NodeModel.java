package com.exoreaction.xorcery.service.neo4j.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import org.neo4j.graphdb.Node;

import java.util.Optional;

public record NodeModel(Node node) {

    public String getString(String name) {
        Object value = node.getProperty(name, null);
        return value == null ? null : value.toString();
    }

    public String getString(Enum<?> name) {
        return getString(name.name());
    }

    public Optional<String> getOptionalString(String name) {
        return Optional.ofNullable(getString(name));
    }

    public Optional<String> getOptionalString(Enum<?> name) {
        return Optional.ofNullable(getString(name.name()));
    }

    public Long getLong(String name) {
        Object value = node.getProperty(name, null);
        return value instanceof Long v ? v : null;
    }

    public Optional<Long> getOptionalLong(String name) {
        return Optional.ofNullable(getLong(name));
    }

    public JsonNode getJsonNode(String name) {
        return Cypher.toJsonNode(node.getProperty(name, null));
    }
}
