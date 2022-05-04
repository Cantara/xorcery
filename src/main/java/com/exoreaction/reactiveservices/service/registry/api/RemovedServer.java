package com.exoreaction.reactiveservices.service.registry.api;

import jakarta.json.JsonValue;

public record RemovedServer(JsonValue json)
        implements RegistryChange
{
}
