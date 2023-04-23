package com.exoreaction.xorcery.health.api;

public enum XorceryHealthStatus {
    HEALTHY(true),
    INITIALIZING(false),
    UNHEALTHY(false),
    PARTIALLY_HEALTHY(false);

    private final boolean healthy;

    XorceryHealthStatus(boolean healthy) {
        this.healthy = healthy;
    }

    public boolean healthy() {
        return healthy;
    }
}
