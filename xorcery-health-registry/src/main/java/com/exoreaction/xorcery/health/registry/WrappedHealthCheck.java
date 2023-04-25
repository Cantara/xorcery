package com.exoreaction.xorcery.health.registry;

import com.exoreaction.xorcery.health.api.HealthCheck;
import com.exoreaction.xorcery.health.api.HealthCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class WrappedHealthCheck implements HealthCheck {
    private static final Logger log = LogManager.getLogger(WrappedHealthCheck.class);

    private final ObjectMapper mapper;
    private final com.codahale.metrics.health.HealthCheck healthCheck;

    public WrappedHealthCheck(ObjectMapper mapper, com.codahale.metrics.health.HealthCheck healthCheck) {
        this.mapper = mapper;
        this.healthCheck = healthCheck;
    }

    @Override
    public HealthCheckResult check() {
        com.codahale.metrics.health.HealthCheck.Result result = healthCheck.execute();
        JsonNode details = null;
        try {
            details = mapper.convertValue(result.getDetails(), JsonNode.class);
        } catch (Throwable t) {
            log.warn("while converting health-check details: " + result.getDetails(), t);
        }
        if (result.isHealthy()) {
            return new HealthCheckResult(details);
        }
        String msg = null;
        Throwable error = null;
        if (result.getError() != null) {
            error = result.getError();
        }
        if (result.getMessage() != null) {
            msg = result.getMessage();
        }
        return new HealthCheckResult(msg, error, details);
    }
}
