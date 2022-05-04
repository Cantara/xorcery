package com.exoreaction.reactiveservices.service.registry.api;

import jakarta.json.JsonValue;

public record AddedServer(JsonValue json)
    implements RegistryChange
{
}
