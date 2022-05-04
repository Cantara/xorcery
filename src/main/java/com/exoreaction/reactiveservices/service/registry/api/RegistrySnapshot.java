package com.exoreaction.reactiveservices.service.registry.api;

import jakarta.json.JsonValue;

public record RegistrySnapshot(JsonValue json)
        implements RegistryChange {
}
