package com.exoreaction.xorcery.health.registry;

import com.codahale.metrics.health.HealthCheck;
import com.exoreaction.xorcery.health.api.XorceryHealthCheck;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckResult;

class HealthCheckAdapter extends HealthCheck {
    private final XorceryHealthCheck healthCheck;

    public HealthCheckAdapter(XorceryHealthCheck healthCheck) {
        this.healthCheck = healthCheck;
    }

    @Override
    protected Result check() {
        XorceryHealthCheckResult result = healthCheck.check();
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
