package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service
public class RootMetricRegistryFactory
        implements Factory<MetricRegistryWrapper> {
    private MetricRegistryWrapper metricRegistryWrapper = new MetricRegistryWrapper(new MetricRegistry());
    private JmxReporter reporter;

    @Inject
    public RootMetricRegistryFactory() {
        reporter = JmxReporter.forRegistry(metricRegistryWrapper.metricRegistry())
                .inDomain("xorcery")
                .build();
        reporter.start();
    }

    @Override
    @Singleton
    @Named("root")
    public MetricRegistryWrapper provide() {
        return metricRegistryWrapper;
    }

    @Override
    public void dispose(MetricRegistryWrapper instance) {
        reporter.stop();
    }
}
