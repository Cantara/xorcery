package com.exoreaction.reactiveservices.disruptor;

import jakarta.json.JsonObject;

import java.util.Optional;

public record RequestMetadata(Metadata metadata) {

    public long timestamp() {
        return metadata.getLong("timestamp").orElse(0L);
    }

    public Optional<String> correlationId()
    {
        return metadata.getString("correlationId");
    }

    public Optional<JsonObject> jwtClaims()
    {
        return metadata.getJsonObject("jwtClaims");
    }

    public Optional<String> remoteIp()
    {
        return metadata.getString("remoteIp");
    }
}
