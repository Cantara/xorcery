package com.exoreaction.xorcery.service.registry.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record RemovedServer(ObjectNode json)
        implements RegistryChange
{
    public ServerResourceDocument server()
    {
        return new ServerResourceDocument(new ResourceDocument(json()));
    }
}
