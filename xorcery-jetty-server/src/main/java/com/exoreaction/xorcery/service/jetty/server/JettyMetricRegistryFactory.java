package com.exoreaction.xorcery.service.jetty.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.service.metricregistry.MetricRegistryWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class JettyMetricRegistryFactory
        implements Factory<MetricRegistryWrapper> {
    private final MetricRegistryWrapper metricRegistry;

    @Inject
    public JettyMetricRegistryFactory(@Named("root") MetricRegistryWrapper rootMetricRegistry) {
        this.metricRegistry = new MetricRegistryWrapper(new MetricRegistry());
        rootMetricRegistry.metricRegistry().register("jetty", metricRegistry.metricRegistry());
    }

    @Override
    @Singleton
    @Named("jetty")
    public MetricRegistryWrapper provide() {
        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistryWrapper instance) {
    }
}
