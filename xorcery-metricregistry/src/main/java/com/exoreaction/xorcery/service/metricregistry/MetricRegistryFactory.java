package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class MetricRegistryFactory
        implements Factory<MetricRegistry> {
    private MetricRegistry metricRegistry = new MetricRegistry();
    private JmxReporter reporter;

    @Inject
    public MetricRegistryFactory() {

        reporter = JmxReporter.forRegistry(metricRegistry)
                .inDomain("xorcery")
                .build();
    }

    @Override
    @Singleton
    @Named("metrics")
    public MetricRegistry provide() {
        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistry instance) {
        reporter.stop();
    }
}
