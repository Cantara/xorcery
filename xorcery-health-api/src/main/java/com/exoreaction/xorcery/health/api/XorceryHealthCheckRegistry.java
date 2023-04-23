package com.exoreaction.xorcery.health.api;

import java.util.function.Supplier;

public interface XorceryHealthCheckRegistry {

    XorceryHealthCheckRegistry register(String componentName, XorceryHealthCheck healthCheck);

    XorceryHealthCheckRegistry registerHealthProbe(String key, Supplier<Object> probe);
}
