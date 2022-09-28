package com.exoreaction.xorcery.service.neo4j.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.graphdb.Result;

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
