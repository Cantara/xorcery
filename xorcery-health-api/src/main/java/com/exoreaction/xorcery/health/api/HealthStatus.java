package com.exoreaction.xorcery.health.api;

public enum HealthStatus {
    HEALTHY(true),
    INITIALIZING(false),
    UNHEALTHY(false),
    PARTIALLY_HEALTHY(false);

    private final boolean healthy;

    HealthStatus(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean healthy() {
        return healthy;
    }
}
