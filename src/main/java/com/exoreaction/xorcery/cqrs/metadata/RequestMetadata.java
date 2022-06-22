package com.exoreaction.xorcery.cqrs.metadata;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public interface RequestMetadata {
    interface Builder<T> {
        Metadata.Builder builder();

        default T correlationId(String value) {
            builder().add("correlationId", value);
            return (T) this;
        }

        default T jwtClaims(ObjectNode value) {
            builder().add("jwtClaims", value);
            return (T) this;
        }

        default T remoteIp(String value) {
            builder().add("remoteIp", value);
            return (T) this;
        }

        default T agent(String value) {
            builder().add("agent", value);
            return (T) this;
        }
    }

    Metadata metadata();

    default Optional<String> getCorrelationId() {
        return metadata().getString("correlationId");
    }

    default Optional<ObjectNode> getJwtClaims() {
        return metadata().getObjectNode("jwtClaims");
    }

    default Optional<String> getRemoteIp() {
        return metadata().getString("remoteIp");
    }

    default Optional<String> getAgent() {
        return metadata().getString("agent");
    }
}
