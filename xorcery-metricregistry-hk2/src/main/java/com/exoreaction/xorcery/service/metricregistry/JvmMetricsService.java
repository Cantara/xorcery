package com.exoreaction.xorcery.service.metricregistry;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.*;
import com.sun.management.UnixOperatingSystemMXBean;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Service(name="jvm")
@RunLevel(4)
public class JvmMetricsService {

    @Inject
    public JvmMetricsService(MetricRegistry metricRegistry) {
        metricRegistry.registerAll("memory", new MemoryUsageGaugeSet());
        metricRegistry.registerAll("threads", new ThreadStatesGaugeSet());
        // jvmMetricRegistry.registerAll("threads", new CachedThreadStatesGaugeSet(5, TimeUnit.SECONDS));
        metricRegistry.registerAll("runtime", new JvmAttributeGaugeSet());
        metricRegistry.registerAll("gc", new GarbageCollectorMetricSet());
        metricRegistry.register("fd", new XorceryFileDescriptorMetricSet());
        metricRegistry.register("classes", new ClassLoadingGaugeSet());
        metricRegistry.registerAll("buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
    }

    public static class XorceryFileDescriptorMetricSet implements MetricSet {
        private static boolean unixOperatingSystemMXBeanExists = false;

        static {
            try {
                Class.forName("com.sun.management.UnixOperatingSystemMXBean");
                unixOperatingSystemMXBeanExists = true;
            } catch (ClassNotFoundException e) {
                // do nothing
            }
        }

        final Map<String, Metric> metricByName;

        public XorceryFileDescriptorMetricSet() {
            final OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
            metricByName = new LinkedHashMap<>();
            if (unixOperatingSystemMXBeanExists && os instanceof UnixOperatingSystemMXBean) {
                metricByName.put("utilization", new FileDescriptorRatioGauge());
                metricByName.put("open", (Gauge<Long>) () -> {
                    final UnixOperatingSystemMXBean unixOs = (UnixOperatingSystemMXBean) os;
                    return unixOs.getOpenFileDescriptorCount();
                });
                metricByName.put("max", (Gauge<Long>) () -> {
                    final UnixOperatingSystemMXBean unixOs = (UnixOperatingSystemMXBean) os;
                    return unixOs.getMaxFileDescriptorCount();
                });
            }
        }

        @Override
        public Map<String, Metric> getMetrics() {
            return metricByName;
        }
    }
}
