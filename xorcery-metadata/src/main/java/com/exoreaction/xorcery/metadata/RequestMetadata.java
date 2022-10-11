package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.WithContext;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public interface RequestMetadata
        extends WithContext<Metadata> {
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

    default Optional<String> getCorrelationId() {
        return context().getString("correlationId");
    }

    default Optional<ObjectNode> getJwtClaims() {
        return context().getJson("jwtClaims").map(ObjectNode.class::cast);
    }

    default Optional<String> getRemoteIp() {
        return context().getString("remoteIp");
    }

    default Optional<String> getAgent() {
        return context().getString("agent");
    }
}
