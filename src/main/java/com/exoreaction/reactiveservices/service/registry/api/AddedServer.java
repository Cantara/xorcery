package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AddedServer(ObjectNode json)
    implements RegistryChange
{
    public ServerResourceDocument server()
    {
        return new ServerResourceDocument(new ResourceDocument(json()));
    }
}
