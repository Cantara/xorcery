package com.exoreaction.reactiveservices.jaxrs.resources;

import com.exoreaction.reactiveservices.cqrs.Model;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.NodeModel;
import com.exoreaction.reactiveservices.service.neo4j.client.RowModel;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.function.Function;

public interface JsonApiModelMixin
        extends ResourceContext {
    default GraphDatabase database() {
        return service(GraphDatabase.class);
    }

    default <T extends Model> Function<RowModel, T> toModel(Function<ObjectNode, T> creator, Class<? extends Enum<?>> fields) {
        return rowModel -> {
            NodeModel node = rowModel.getNode(fields.getSimpleName());
            ObjectNode json = JsonNodeFactory.instance.objectNode();

            for (Enum<?> enumConstant : fields.getEnumConstants()) {
                json.set(Cypher.toField(enumConstant), node.getJsonNode(enumConstant.name()));
            }

            return creator.apply(json);
        };
    }
}
