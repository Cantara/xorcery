package com.exoreaction.xorcery.admin.servlet;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.ClassLoadingGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.exoreaction.xorcery.service.metricregistry.MetricRegistryWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.ManagementFactory;

@Service
public class JvmMetricRegistryFactory
        implements Factory<MetricRegistryWrapper> {
    private final MetricRegistryWrapper metricRegistryWrapper;

    @Inject
    public JvmMetricRegistryFactory(@Named("root") MetricRegistryWrapper rootMetricRegistry) {
        MetricRegistry metricRegistry = new MetricRegistry();
        metricRegistryWrapper = new MetricRegistryWrapper(metricRegistry);
        metricRegistry.registerAll("memory", new MemoryUsageGaugeSet());
        metricRegistry.registerAll("threads", new ThreadStatesGaugeSet());
        // jvmMetricRegistry.registerAll("threads", new CachedThreadStatesGaugeSet(5, TimeUnit.SECONDS));
        metricRegistry.registerAll("runtime", new JvmAttributeGaugeSet());
        metricRegistry.registerAll("gc", new GarbageCollectorMetricSet());
        metricRegistry.register("fd", new XorceryFileDescriptorMetricSet());
        metricRegistry.register("classes", new ClassLoadingGaugeSet());
        metricRegistry.registerAll("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
        rootMetricRegistry.metricRegistry().register("jvm", metricRegistry);
    }

    @Override
    @Singleton
    @Named("jvm")
    public MetricRegistryWrapper provide() {
        return metricRegistryWrapper;
    }

    @Override
    public void dispose(MetricRegistryWrapper instance) {
    }
}
