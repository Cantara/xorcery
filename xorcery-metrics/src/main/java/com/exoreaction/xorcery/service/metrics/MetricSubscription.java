package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.*;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;

import java.util.Collection;
import java.util.concurrent.Flow;

class MetricSubscription
        implements Flow.Subscription {

    private final Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber;
    private Collection<String> metricNames;
    private MetricRegistry metricRegistry;
    private DeploymentMetadata deploymentMetadata;

    public MetricSubscription(DeploymentMetadata deploymentMetadata, Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber, Collection<String> metricNames, MetricRegistry metricRegistry) {
        this.deploymentMetadata = deploymentMetadata;
        this.metricNames = metricNames;
        this.metricRegistry = metricRegistry;
        this.subscriber = subscriber;
        subscriber.onSubscribe(this);
    }

    @Override
    public void request(long n) {
        ObjectNode metricsBuilder = JsonNodeFactory.instance.objectNode();
        for (String metricName : metricNames) {
            Metric metric = metricRegistry.getMetrics().get(metricName);
            try {
                if (metric instanceof Gauge gauge) {
                    Number value = (Number) gauge.getValue();
                    if (value instanceof Double v) {
                        if (Double.isNaN((Double) value))
                            continue; // Skip these

                        metricsBuilder.set(metricName, metricsBuilder.numberNode(v));
                    } else if (value instanceof Float v) {
                        metricsBuilder.set(metricName, metricsBuilder.numberNode(v));
                    } else if (value instanceof Long v) {
                        metricsBuilder.set(metricName, metricsBuilder.numberNode(v));
                    } else if (value instanceof Integer v) {
                        metricsBuilder.set(metricName, metricsBuilder.numberNode(v));
                    }
                } else if (metric instanceof Meter meter) {
                    ObjectNode builder = JsonNodeFactory.instance.objectNode();
                    builder.set("count", builder.numberNode(meter.getCount()));
                    builder.set("meanrate", builder.numberNode(meter.getMeanRate()));
                    metricsBuilder.set(metricName, builder);
                } else if (metric instanceof Counter counter) {
                    metricsBuilder.set(metricName, metricsBuilder.numberNode(counter.getCount()));
                } else {
//                        System.out.println(metric.getClass());
                }
            } catch (Throwable e) {
                LogManager.getLogger(getClass()).error("Could not serialize metric " + metricName + "with value " + metric.toString(), e);
            }
        }

        subscriber.onNext(new WithMetadata<>(new Metadata.Builder(deploymentMetadata.metadata().metadata().deepCopy())
                .add("timestamp", System.currentTimeMillis())
                .build(), metricsBuilder));
    }

    @Override
    public void cancel() {
        System.out.println("METRIC SUBSCRIPTION CANCELLED");
        subscriber.onComplete();
    }
}
