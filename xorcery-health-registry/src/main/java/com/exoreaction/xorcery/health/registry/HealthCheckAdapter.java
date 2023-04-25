package com.exoreaction.xorcery.health.registry;

import com.exoreaction.xorcery.health.api.HealthCheck;
import com.exoreaction.xorcery.health.api.HealthCheckResult;

class HealthCheckAdapter extends com.codahale.metrics.health.HealthCheck {
    private final HealthCheck healthCheck;

    public HealthCheckAdapter(HealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    @Override
    protected Result check() {
        HealthCheckResult result = healthCheck.check();
        ResultBuilder builder = Result.builder();
        if (result.status().healthy()) {
            builder.healthy();
        } else {
            if (result.unhealthyThrowable() != null) {
                builder.unhealthy(result.unhealthyThrowable());
            } else {
                builder.unhealthy();
            }
            if (result.unhealthyMessage() != null && !result.unhealthyMessage().isBlank()) {
                builder.withMessage(result.unhealthyMessage());
            }
        }
        builder.withDetail("info", result.info());
        return builder.build();
    }
}
