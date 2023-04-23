package com.exoreaction.xorcery.admin.servlet;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jvm.FileDescriptorRatioGauge;
import com.sun.management.UnixOperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

public class XorceryFileDescriptorMetricSet implements MetricSet {
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
