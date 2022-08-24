package com.exoreaction.xorcery.service.neo4j.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Result;

import java.util.List;
import java.util.Map;

public record RowModel(Result.ResultRow row) {

    public JsonNode getJsonNode(String name) {
        return Cypher.toJsonNode(row.get(name));
    }

    public NodeModel getNode(String name) {
        return new NodeModel(row.getNode(name));
    }

    public NodeModel getNode(Enum<?> name) {
        return new NodeModel(row.getNode(name.name()));
    }
}
