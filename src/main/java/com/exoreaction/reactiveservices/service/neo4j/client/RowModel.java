package com.exoreaction.reactiveservices.service.neo4j.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import org.neo4j.graphdb.Result;

public record RowModel(Result.ResultRow row) {

    public JsonNode getJsonNode(String name) {
        Object value = row.get(name);
        if (value == null) {
            return NullNode.getInstance();
        } else if (value instanceof String v) {
            return JsonNodeFactory.instance.textNode(v);
        } else if (value instanceof Long v) {
            return JsonNodeFactory.instance.numberNode(v);
        } else if (value instanceof Double v) {
            return JsonNodeFactory.instance.numberNode(v);
        } else if (value instanceof Boolean v) {
            return JsonNodeFactory.instance.booleanNode(v);
        } else {
            return null;
        }
    }

    public NodeModel getNode(String name) {
        return new NodeModel(row.getNode(name));
    }

    public NodeModel getNode(Enum<?> name) {
        return new NodeModel(row.getNode(name.name()));
    }
}
