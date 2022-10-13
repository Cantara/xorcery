package com.exoreaction.xorcery.service.registry.api;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;

public record RegistrySnapshot(ArrayNode json)
        implements RegistryChange {

    public Collection<ServerResourceDocument> servers() {
        return JsonElement.getValuesAs(json(), j -> new ServerResourceDocument(new ResourceDocument((ObjectNode) j)));
    }
}
