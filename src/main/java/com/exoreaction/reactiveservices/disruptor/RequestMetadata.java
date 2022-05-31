package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public record RequestMetadata(Metadata metadata) {

    public long timestamp() {
        return metadata.getLong("timestamp").orElse(0L);
    }

    public Optional<String> correlationId()
    {
        return metadata.getString("correlationId");
    }

    public Optional<ObjectNode> jwtClaims()
    {
        return metadata.getObjectNode("jwtClaims");
    }

    public Optional<String> remoteIp()
    {
        return metadata.getString("remoteIp");
    }
}
