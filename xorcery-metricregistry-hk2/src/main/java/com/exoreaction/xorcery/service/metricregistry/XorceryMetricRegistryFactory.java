package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class XorceryMetricRegistryFactory
        implements Factory<MetricRegistryWrapper> {
    private final MetricRegistryWrapper metricRegistryWrapper;

    @Inject
    public XorceryMetricRegistryFactory(@Named("root") MetricRegistryWrapper rootMetricRegistry) {
        this.metricRegistryWrapper = new MetricRegistryWrapper(new MetricRegistry());
        rootMetricRegistry.metricRegistry().register("xorcery", metricRegistryWrapper.metricRegistry());
    }

    @Override
    @Singleton
    @Named("xorcery")
    public MetricRegistryWrapper provide() {
        return metricRegistryWrapper;
    }

    @Override
    public void dispose(MetricRegistryWrapper instance) {
    }
}
