package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.exoreaction.util.JsonNodes;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;

public record RegistrySnapshot(ObjectNode json)
        implements RegistryChange {

    public Collection<ServerResourceDocument> servers() {
        return JsonNodes.getValuesAs(json(), json -> new ServerResourceDocument(new ResourceDocument((ObjectNode) json)));
    }
}
