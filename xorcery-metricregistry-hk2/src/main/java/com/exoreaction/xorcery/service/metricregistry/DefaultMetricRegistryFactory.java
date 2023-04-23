package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class DefaultMetricRegistryFactory
        implements Factory<MetricRegistry> {
    private final MetricRegistry metricRegistry;

    @Inject
    public DefaultMetricRegistryFactory(@Named("app") MetricRegistryWrapper wrapper) {
        this.metricRegistry = wrapper.metricRegistry();
    }

    @Override
    @Singleton
    public MetricRegistry provide() {
        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistry instance) {
    }
}
