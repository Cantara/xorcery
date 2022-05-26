package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import jakarta.json.JsonValue;

public record AddedServer(JsonValue json)
    implements RegistryChange
{
    public ServerResourceDocument server()
    {
        return new ServerResourceDocument(new ResourceDocument(json().asJsonObject()));
    }
}
