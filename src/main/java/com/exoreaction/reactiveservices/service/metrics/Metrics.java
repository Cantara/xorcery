package com.exoreaction.reactiveservices.service.metrics;

import com.codahale.metrics.*;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Singleton
public class Metrics
        implements ContainerLifecycleListener, ReactiveEventStreams.Publisher<ObjectNode> {

    public static final String SERVICE_TYPE = "metrics";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("metrics", "api/metrics")
                    .websocket("metricevents", "ws/metricevents");
        }

        @Override
        protected void configure() {
            context.register(Metrics.class, ContainerLifecycleListener.class);
        }
    }

    private ServiceResourceObject resourceObject;
    private final ReactiveStreams reactiveStreams;
    private MetricRegistry metricRegistry;

    private final DeploymentMetadata deploymentMetadata;

    @Inject
    public Metrics(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                   ReactiveStreams reactiveStreams,
                   MetricRegistry metricRegistry,
                   Configuration configuration) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        this.metricRegistry = metricRegistry;
        this.deploymentMetadata = new DeploymentMetadata(configuration);
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.getLinkByRel("metricevents").ifPresent(link ->
        {
            reactiveStreams.publish(resourceObject.serviceIdentifier(), link, this);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<ObjectNode> subscriber, ObjectNode parameters) {
        String metricNames = parameters.path("metrics").get(0).asText("");
        Collection<String> metricNamesList = metricNames.isBlank() ? metricRegistry.getNames() : Arrays.asList(metricNames.split(","));

        new MetricSubscription(subscriber, metricNamesList, metricRegistry);
    }

    private class MetricSubscription
            implements ReactiveEventStreams.Subscription {
        private final CompletableFuture<EventSink<Event<ObjectNode>>> eventSink = new CompletableFuture<>();
        private final ReactiveEventStreams.Subscriber<ObjectNode> subscriber;
        private Collection<String> metricNames;
        private MetricRegistry metricRegistry;

        public MetricSubscription(ReactiveEventStreams.Subscriber<ObjectNode> subscriber, Collection<String> metricNames, MetricRegistry metricRegistry) {
            this.metricNames = metricNames;
            this.metricRegistry = metricRegistry;
            this.subscriber = subscriber;
            this.eventSink.complete(subscriber.onSubscribe(this));
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

            try {
                eventSink.whenComplete((es, t) ->
                {
                    es.publishEvent((e, seq, metric) ->
                    {
                        e.metadata = deploymentMetadata.metadata();
                        e.event = metric;
                    }, metricsBuilder);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void cancel() {
            System.out.println("METRIC SUBSCRIPTION CANCELLED");
            subscriber.onComplete();
        }
    }
}
