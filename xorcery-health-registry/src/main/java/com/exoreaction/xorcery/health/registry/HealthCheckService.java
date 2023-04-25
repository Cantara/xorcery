package com.exoreaction.xorcery.health.registry;

import com.exoreaction.xorcery.health.api.HealthCheckRegistry;
import com.exoreaction.xorcery.health.api.HealthCheckResult;

import java.util.SortedMap;

public interface HealthCheckService extends HealthCheckRegistry {

    SortedMap<String, HealthCheckResult> runHealthChecks();

    com.codahale.metrics.health.HealthCheckRegistry codahaleRegistry();
}
