package com.exoreaction.reactiveservices.jsonapi.resources;

import com.exoreaction.reactiveservices.cqrs.model.Model;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.RowModel;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.function.Function;

public interface ModelsMixin
        extends ResourceContext {
    default GraphDatabase database() {
        return service(GraphDatabase.class);
    }

    default <T extends Model> Function<RowModel, T> toModel(Function<ObjectNode, T> creator, Collection<Enum<?>> fields) {
        return rowModel -> {
            ObjectNode json = JsonNodeFactory.instance.objectNode();
            for (Enum<?> enumConstant : fields) {
                json.set(Cypher.toField(enumConstant), rowModel.getJsonNode(Cypher.toField(enumConstant)));
            }
            return creator.apply(json);
        };
    }
}
