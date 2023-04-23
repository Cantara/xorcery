package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class AppMetricRegistryFactory
        implements Factory<MetricRegistryWrapper> {
    private final MetricRegistryWrapper metricRegistry;

    @Inject
    public AppMetricRegistryFactory(@Named("root") MetricRegistryWrapper baseMetricRegistry) {
        this.metricRegistry = new MetricRegistryWrapper(new MetricRegistry());
        baseMetricRegistry.metricRegistry().register("app", metricRegistry.metricRegistry());
    }

    @Override
    @Singleton
    @Named("app")
    public MetricRegistryWrapper provide() {
        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistryWrapper instance) {
    }
}
