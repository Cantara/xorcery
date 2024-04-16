package com.exoreaction.xorcery.opentelemetry.sdk.exporters.jmx;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.*;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.apache.logging.log4j.Logger;

import javax.management.*;
import javax.management.modelmbean.*;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class JMXMetricExporter implements MetricExporter {
    private final AtomicBoolean isShutdown;
    private final AggregationTemporality aggregationTemporality;
    private final MBeanServer managementServer;
    private final Map<Resource, Map<Attributes, ResourceDynamicMBean>> registeredMBeans = new HashMap<>();
    private final Logger logger;

    public JMXMetricExporter(AggregationTemporality aggregationTemporality, Logger logger) {
        this.logger = logger;
        this.isShutdown = new AtomicBoolean();
        this.aggregationTemporality = aggregationTemporality;
        this.managementServer = ManagementFactory.getPlatformMBeanServer();
    }

    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return this.aggregationTemporality;
    }

    public CompletableResultCode export(Collection<MetricData> metrics) {
        if (this.isShutdown.get()) {
            return CompletableResultCode.ofFailure();
        } else {
            try {
                // Calculate what resources we are managing
                for (MetricData metric : metrics) {
                    switch (metric.getType()) {
                        case LONG_GAUGE -> {
                            for (LongPointData lpd : metric.getLongGaugeData().getPoints()) {
                                getResourceDynamicMBean(metric, lpd).setMetric(metric, lpd.getValue());
                            }
                        }
                        case DOUBLE_GAUGE -> {
                            for (DoublePointData doublePointData : metric.getDoubleGaugeData().getPoints()) {
                                getResourceDynamicMBean(metric, doublePointData).setMetric(metric, doublePointData.getValue());
                            }
                        }
                        case LONG_SUM -> {
                            for (LongPointData longPointData : metric.getLongSumData().getPoints()) {
                                getResourceDynamicMBean(metric, longPointData).setMetric(metric, longPointData.getValue());
                            }
                        }
                        case DOUBLE_SUM -> {
                            for (DoublePointData doublePointData : metric.getDoubleSumData().getPoints()) {
                                getResourceDynamicMBean(metric, doublePointData).setMetric(metric, doublePointData.getValue());
                            }
                        }
                        case SUMMARY -> {
                        }
                        case HISTOGRAM -> {
                        }
                        case EXPONENTIAL_HISTOGRAM -> {
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("Could not update metrics in JMX", e);
                return CompletableResultCode.ofFailure();
            }

            return CompletableResultCode.ofSuccess();
        }
    }

    private ResourceDynamicMBean getResourceDynamicMBean(MetricData metric, PointData pointData) throws MalformedObjectNameException, InstanceAlreadyExistsException, MBeanRegistrationException, NotCompliantMBeanException {
        Map<Attributes, ResourceDynamicMBean> attributesResourceDynamicMBeanMap = registeredMBeans.computeIfAbsent(metric.getResource(), r -> new HashMap<>());
        ResourceDynamicMBean resourceMbean = attributesResourceDynamicMBeanMap.get(pointData.getAttributes());
        if (resourceMbean == null) {
            ModelMBeanInfoSupport mBeanInfoSupport = new ModelMBeanInfoSupport(
                    RequiredModelMBean.class.getName(),
                    pointData.getAttributes().toString(),
                    new ModelMBeanAttributeInfo[0],
                    new ModelMBeanConstructorInfo[0],
                    new ModelMBeanOperationInfo[0],
                    new ModelMBeanNotificationInfo[0]
            );

            resourceMbean = new ResourceDynamicMBean(mBeanInfoSupport);

            String resourceObjectName = metric.getResource().getAttributes().asMap().entrySet().stream()
                    .filter(entry -> entry.getKey().getKey().equals("service.instance.id"))
                    .map(entry -> entry.getKey().getKey() + "=" + entry.getValue().toString()).collect(Collectors.joining(","));
            if (!pointData.getAttributes().isEmpty())
            {
                resourceObjectName += ","+ pointData.getAttributes().asMap().entrySet().stream()
                        .map(entry -> entry.getKey().getKey() + "=\"" + entry.getValue().toString()+"\"").collect(Collectors.joining(","));
            }
            ObjectName objectName = new ObjectName("opentelemetry:" + resourceObjectName );
            managementServer.registerMBean(resourceMbean, objectName);
            attributesResourceDynamicMBeanMap.put(pointData.getAttributes(), resourceMbean);
        }
        return resourceMbean;
    }

    public CompletableResultCode flush() {
        CompletableResultCode resultCode = new CompletableResultCode();
        return resultCode.succeed();
    }

    public CompletableResultCode shutdown() {
        if (!this.isShutdown.compareAndSet(false, true)) {
            return CompletableResultCode.ofSuccess();
        } else {
            return this.flush();
        }
    }

    public String toString() {
        return "JMXMetricExporter";
    }
}
