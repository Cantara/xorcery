package com.exoreaction.xorcery.neo4j.jsonapi.resources;

import com.exoreaction.xorcery.jsonapi.server.resources.ResourceContext;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.RowModel;
import com.exoreaction.xorcery.util.Enums;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.function.Function;

public interface ModelsMixin
        extends ResourceContext {
    default GraphDatabase database() {
        return service(GraphDatabase.class);
    }

    default <T> Function<RowModel, T> toModel(Function<ObjectNode, T> creator, Collection<Enum<?>> fields) {
        return rowModel -> {
            ObjectNode json = JsonNodeFactory.instance.objectNode();
            for (Enum<?> enumConstant : fields) {
                json.set(Enums.toField(enumConstant), rowModel.getJsonNode(Enums.toField(enumConstant)));
            }
            return creator.apply(json);
        };
    }
}
