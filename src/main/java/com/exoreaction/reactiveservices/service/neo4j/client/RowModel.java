package com.exoreaction.reactiveservices.service.neo4j.client;

import org.neo4j.graphdb.Result;

public record RowModel(Result.ResultRow row) {

    public NodeModel getNode(String name) {
        return new NodeModel(row.getNode(name));
    }

    public NodeModel getNode(Enum<?> name) {
        return new NodeModel(row.getNode(name.name()));
    }
}
