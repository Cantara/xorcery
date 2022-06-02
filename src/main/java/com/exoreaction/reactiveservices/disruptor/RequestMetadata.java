package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public record RequestMetadata(Metadata metadata) {

    public record Builder(Metadata.Builder metadata) {
        public Builder timestamp(long timestamp) {
            metadata.add("timestamp", timestamp);
            return this;
        }

        public Builder remoteIp(String remoteIp) {
            metadata.add("remoteIp", remoteIp);
            return this;
        }

        public Builder agent(String agent) {
            metadata.add("agent", agent);
            return this;
        }

        public RequestMetadata build() {
            return new RequestMetadata(metadata.build());
        }

    }

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

    public Optional<String> agent() {
        return metadata.getString("agent");
    }

}
