package com.exoreaction.xorcery.health.api;

import com.fasterxml.jackson.databind.JsonNode;

public record HealthCheckResult(HealthStatus status, String unhealthyMessage, Throwable unhealthyThrowable, JsonNode info) {

    public HealthCheckResult() {
        this(HealthStatus.HEALTHY, null, null, null);
    }

    public HealthCheckResult(JsonNode info) {
        this(HealthStatus.HEALTHY, null, null, info);
    }

    public HealthCheckResult(String unhealthyMessage) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, null, null);
    }

    public HealthCheckResult(String unhealthyMessage, JsonNode info) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, null, info);
    }

    public HealthCheckResult(Throwable unhealthyThrowable, JsonNode info) {
        this(HealthStatus.UNHEALTHY, "", unhealthyThrowable, info);
    }

    public HealthCheckResult(String unhealthyMessage, Throwable unhealthyThrowable) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, unhealthyThrowable, null);
    }

    public HealthCheckResult(String unhealthyMessage, Throwable unhealthyThrowable, JsonNode info) {
        this(HealthStatus.UNHEALTHY, unhealthyMessage, unhealthyThrowable, info);
    }
}
