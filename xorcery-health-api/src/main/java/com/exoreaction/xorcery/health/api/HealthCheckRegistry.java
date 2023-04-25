package com.exoreaction.xorcery.health.api;

import java.util.function.Supplier;

public interface HealthCheckRegistry {

    HealthCheckRegistry register(String componentName, HealthCheck healthCheck);

    HealthCheckRegistry registerHealthProbe(String key, Supplier<Object> probe);

    void setVersion(String version);
}
