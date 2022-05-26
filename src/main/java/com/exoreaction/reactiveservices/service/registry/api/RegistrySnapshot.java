package com.exoreaction.reactiveservices.service.registry.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Collection;

public record RegistrySnapshot(JsonValue json)
        implements RegistryChange {

    public Collection<ServerResourceDocument> servers() {
        return json().asJsonArray().getValuesAs(json -> new ServerResourceDocument(new ResourceDocument((JsonObject) json)));
    }
}
