package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;

import javax.management.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class JmxMetricSubscription
        implements Flow.Subscription {

    private final Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber;
    private Collection<String> metricNames;
    private MetricRegistry metricRegistry;
    private Duration delay;
    private ScheduledExecutorService executorService;
    private DeploymentMetadata deploymentMetadata;
    private final Optional<List<String>> filters;
    private final MBeanServer managementServer;
    private final ScheduledFuture<?> scheduledFuture;
    private final Set<MBeanAttributeInfo> unsupportedOperationAttributes = new HashSet<>();

    public JmxMetricSubscription(Duration delay, ScheduledExecutorService executorService, DeploymentMetadata deploymentMetadata, Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber, Optional<List<String>> filters, MBeanServer managementServer) {
        this.delay = delay;
        this.executorService = executorService;
        this.deploymentMetadata = deploymentMetadata;
        this.filters = filters;
        this.managementServer = managementServer;
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);

        scheduledFuture = executorService.scheduleWithFixedDelay(() ->
        {
            try {
                ObjectNode metricsBuilder = JsonNodeFactory.instance.objectNode();
                filters.ifPresentOrElse(list ->
                {
                    for (String filter : list) {
                        addAttributes(managementServer, filter, metricsBuilder);
                    }
                }, () ->
                {
                    addAttributes(managementServer, null, metricsBuilder);
                });

                subscriber.onNext(new WithMetadata<>(new Metadata.Builder(deploymentMetadata.context().metadata().deepCopy())
                        .add("timestamp", System.currentTimeMillis())
                        .build(), metricsBuilder));
            } catch (Throwable e) {
                LogManager.getLogger(getClass()).error("Could not send metrics", e);
            }
        }, 0, delay.toSeconds(), TimeUnit.SECONDS);
    }

    private void addAttributes(MBeanServer managementServer, String filter, ObjectNode metricsBuilder) {
        try {
            ObjectName filterName = filter == null ? null : ObjectName.getInstance(filter);
            Set<ObjectName> objectNameSet = managementServer.queryNames(filterName, null);
            for (ObjectName objectName : objectNameSet) {
                MBeanInfo mBeanInfo = managementServer.getMBeanInfo(objectName);

                ObjectNode mbeanMetricsBuilder = JsonNodeFactory.instance.objectNode();
                for (MBeanAttributeInfo attribute : mBeanInfo.getAttributes()) {

                    if (unsupportedOperationAttributes.contains(attribute))
                        continue;

                    try {
                        Object value = managementServer.getAttribute(objectName, attribute.getName());
                        if (value != null) {
                            mbeanMetricsBuilder.set(attribute.getName(), mbeanMetricsBuilder.textNode(value.toString()));
                        }
                    } catch (RuntimeMBeanException e) {
                        if (e.getCause() instanceof UnsupportedOperationException) {
                            unsupportedOperationAttributes.add(attribute);
                        }
                    } catch (Throwable e) {
                        LogManager.getLogger(getClass()).error("Could not get metrics for {}: {}", objectName, attribute.getName(), e);
                        unsupportedOperationAttributes.add(attribute);
                    }
                }
                if (!mbeanMetricsBuilder.isEmpty()) {
                    metricsBuilder.set(objectName.getCanonicalName(), mbeanMetricsBuilder);
                }
            }
        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not get metrics", e);
        }
    }

    @Override
    public void request(long n) {
    }

    @Override
    public void cancel() {
        LogManager.getLogger(getClass()).info("Metrics subscription cancelled");
        scheduledFuture.cancel(false);
        subscriber.onComplete();
    }
}
