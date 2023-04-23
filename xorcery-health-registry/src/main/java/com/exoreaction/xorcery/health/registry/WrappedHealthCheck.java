package com.exoreaction.xorcery.health.registry;

import com.codahale.metrics.health.HealthCheck;
import com.exoreaction.xorcery.health.api.XorceryHealthCheck;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class WrappedHealthCheck implements XorceryHealthCheck {
    private static final Logger log = LogManager.getLogger(WrappedHealthCheck.class);

    private final ObjectMapper mapper;
    private final HealthCheck healthCheck;

    public WrappedHealthCheck(ObjectMapper mapper, HealthCheck healthCheck) {
        this.mapper = mapper;
        this.healthCheck = healthCheck;
    }

    @Override
    public XorceryHealthCheckResult check() {
        HealthCheck.Result result = healthCheck.execute();
        JsonNode details = null;
        try {
            details = mapper.convertValue(result.getDetails(), JsonNode.class);
        } catch (Throwable t) {
            log.warn("while converting health-check details: " + result.getDetails(), t);
        }
        if (result.isHealthy()) {
            return new XorceryHealthCheckResult(details);
        }
        String msg = null;
        Throwable error = null;
        if (result.getError() != null) {
            error = result.getError();
        }
        if (result.getMessage() != null) {
            msg = result.getMessage();
        }
        return new XorceryHealthCheckResult(msg, error, details);
    }
}
