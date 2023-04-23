package com.exoreaction.xorcery.health.api;

import com.fasterxml.jackson.databind.JsonNode;

public record XorceryHealthCheckResult(XorceryHealthStatus status, String unhealthyMessage, Throwable unhealthyThrowable, JsonNode info) {

    public XorceryHealthCheckResult() {
        this(XorceryHealthStatus.HEALTHY, null, null, null);
    }

    public XorceryHealthCheckResult(JsonNode info) {
        this(XorceryHealthStatus.HEALTHY, null, null, info);
    }

    public XorceryHealthCheckResult(String unhealthyMessage) {
        this(XorceryHealthStatus.UNHEALTHY, unhealthyMessage, null, null);
    }

    public XorceryHealthCheckResult(String unhealthyMessage, JsonNode info) {
        this(XorceryHealthStatus.UNHEALTHY, unhealthyMessage, null, info);
    }

    public XorceryHealthCheckResult(Throwable unhealthyThrowable, JsonNode info) {
        this(XorceryHealthStatus.UNHEALTHY, "", unhealthyThrowable, info);
    }

    public XorceryHealthCheckResult(String unhealthyMessage, Throwable unhealthyThrowable) {
        this(XorceryHealthStatus.UNHEALTHY, unhealthyMessage, unhealthyThrowable, null);
    }

    public XorceryHealthCheckResult(String unhealthyMessage, Throwable unhealthyThrowable, JsonNode info) {
        this(XorceryHealthStatus.UNHEALTHY, unhealthyMessage, unhealthyThrowable, info);
    }
}
