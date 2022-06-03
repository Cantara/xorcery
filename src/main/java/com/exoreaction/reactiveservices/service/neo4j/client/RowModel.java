package com.exoreaction.reactiveservices.service.neo4j.client;

import com.exoreaction.reactiveservices.service.forum.model.ForumModel;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;

import java.util.Optional;

public record RowModel(Result.ResultRow row) {

    public NodeModel getNode(String name) {
        return new NodeModel(row.getNode(name));
    }

    public NodeModel getNode(Enum<?> name) {
        return new NodeModel(row.getNode(name.name()));
    }
}
