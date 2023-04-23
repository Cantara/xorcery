package com.exoreaction.xorcery.health.registry;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckRegistry;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckResult;

import java.util.SortedMap;

public interface XorceryHealthCheckService extends XorceryHealthCheckRegistry {

    SortedMap<String, XorceryHealthCheckResult> runHealthChecks();

    HealthCheckRegistry codahaleRegistry();
}
