/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.opentelemetry.exporters.jmx;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class JMXMetricExporter implements MetricExporter {
    private final AtomicBoolean isShutdown;
    private final AggregationTemporality aggregationTemporality;
    private final MBeanServer managementServer;
    private final Map<Resource, Map<List<Object>, ResourceDynamicMBean>> registeredMBeans = new HashMap<>();
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
                            for (SummaryPointData summaryPointData : metric.getSummaryData().getPoints()) {
                                getResourceDynamicMBean(metric, summaryPointData).setMetric(metric, summaryPointData);
                            }
                        }
                        case HISTOGRAM -> {
                            for (HistogramPointData histogramPointData : metric.getHistogramData().getPoints()) {
                                getResourceDynamicMBean(metric, histogramPointData).setMetric(metric, histogramPointData);
                            }
                        }
                        case EXPONENTIAL_HISTOGRAM -> {
                            for (ExponentialHistogramPointData histogramPointData : metric.getExponentialHistogramData().getPoints()) {
                                getResourceDynamicMBean(metric, histogramPointData).setMetric(metric, histogramPointData);
                            }
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
        Map<List<Object>, ResourceDynamicMBean> attributesResourceDynamicMBeanMap = registeredMBeans.computeIfAbsent(metric.getResource(), r -> new HashMap<>());
        ResourceDynamicMBean resourceMbean = attributesResourceDynamicMBeanMap.get(List.of(metric.getInstrumentationScopeInfo(), pointData.getAttributes()));
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

            // The ObjectName is derived from resource -> scope -> attributes
            String resourceObjectName = metric.getResource().getAttributes().asMap().entrySet().stream()
                    .filter(entry -> entry.getKey().getKey().equals("service.instance.id"))
                    .map(entry -> entry.getKey().getKey() + "=" + entry.getValue().toString()).collect(Collectors.joining(","));

            InstrumentationScopeInfo instrumentationScopeInfo = metric.getInstrumentationScopeInfo();
            resourceObjectName += ",scope="+ instrumentationScopeInfo.getName();
            if (!instrumentationScopeInfo.getAttributes().isEmpty())
            {
                resourceObjectName += ","+ instrumentationScopeInfo.getAttributes().asMap().entrySet().stream()
                        .map(entry -> entry.getKey().getKey() + "=\"" + entry.getValue().toString().replace("\\", "\\\\")+"\"").collect(Collectors.joining(","));
            }

            if (!pointData.getAttributes().isEmpty())
            {
                resourceObjectName += ","+ pointData.getAttributes().asMap().entrySet().stream()
                        .map(entry -> entry.getKey().getKey() + "=\"" + entry.getValue().toString().replace("\\", "\\\\")+"\"").collect(Collectors.joining(","));
            }

            try {
                ObjectName objectName = new ObjectName("opentelemetry:" + resourceObjectName );
                managementServer.registerMBean(resourceMbean, objectName);
                attributesResourceDynamicMBeanMap.put(List.of(metric.getInstrumentationScopeInfo(), pointData.getAttributes()), resourceMbean);
            } catch (MalformedObjectNameException e) {
                throw new RuntimeException("Invalid name:"+resourceObjectName, e);
            }
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
