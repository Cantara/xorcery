package com.exoreaction.xorcery.service.jersey.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.service.metricregistry.MetricRegistryWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class JerseyMetricRegistryFactory
        implements Factory<MetricRegistryWrapper> {
    private final MetricRegistryWrapper metricRegistry;

    @Inject
    public JerseyMetricRegistryFactory(@Named("root") MetricRegistryWrapper baseMetricRegistry) {
        this.metricRegistry = new MetricRegistryWrapper(new MetricRegistry());
        baseMetricRegistry.metricRegistry().register("jersey", metricRegistry.metricRegistry());
    }

    @Override
    @Singleton
    @Named("jersey")
    public MetricRegistryWrapper provide() {
        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistryWrapper instance) {
    }
}
