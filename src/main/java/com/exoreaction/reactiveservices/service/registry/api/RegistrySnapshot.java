package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.exoreaction.util.json.JsonNodes;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;

public record RegistrySnapshot(ArrayNode json)
        implements RegistryChange {

    public Collection<ServerResourceDocument> servers() {
        return JsonNodes.getValuesAs(json(), j -> new ServerResourceDocument(new ResourceDocument((ObjectNode) j)));
    }
}
