package com.exoreaction.xorcery.service.neo4j.client;

import com.exoreaction.xorcery.util.Enums;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Result;

import java.util.Collection;
import java.util.function.Function;

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

    public <T> T toModel(Function<ObjectNode, T> creator, Enum<?>... fields) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        for (Enum<?> enumConstant : fields) {
            json.set(Enums.toField(enumConstant), this.getJsonNode(Enums.toField(enumConstant)));
        }
        return creator.apply(json);
    }
}
