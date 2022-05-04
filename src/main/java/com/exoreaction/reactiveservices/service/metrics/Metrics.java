package com.exoreaction.reactiveservices.service.metrics;

import com.codahale.metrics.*;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
public class Metrics
        implements ContainerLifecycleListener, ReactiveEventStreams.Publisher<JsonObject> {

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
                    .websocket("metricevents", "ws/metricevents", "metrics={metric_names}");
        }

        @Override
        protected void configure() {
            context.register(Metrics.class, ContainerLifecycleListener.class);
        }
    }

    private ServiceResourceObject resourceObject;
    private final ReactiveStreams reactiveStreams;
    private MetricRegistry metricRegistry;

    @Inject
    public Metrics(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject, ReactiveStreams reactiveStreams, MetricRegistry metricRegistry) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.resourceObject().getLinks().getRel("metricevents").ifPresent(link ->
        {
            reactiveStreams.publish(resourceObject.serviceReference().link("metricevents"), this, link);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<JsonObject> subscriber, Map<String, String> parameters) {
        String metricNames = Optional.ofNullable(parameters.get("metrics")).orElse("");
        Collection<String> metricNamesList = metricNames.isBlank() ? metricRegistry.getNames() : Arrays.asList(metricNames.split(","));

        new MetricSubscription(subscriber, metricNamesList, metricRegistry);
    }

    private class MetricSubscription
            implements ReactiveEventStreams.Subscription {
        private final CompletableFuture<EventSink<Event<JsonObject>>> subscriber = new CompletableFuture<>();
        private Collection<String> metricNames;
        private MetricRegistry metricRegistry;

        public MetricSubscription(ReactiveEventStreams.Subscriber<JsonObject> subscriber, Collection<String> metricNames, MetricRegistry metricRegistry) {
            this.metricNames = metricNames;
            this.metricRegistry = metricRegistry;
            this.subscriber.complete( subscriber.onSubscribe(this));
        }

        @Override
        public void request(long n) {
            JsonObjectBuilder metricsBuilder = Json.createObjectBuilder();
            for (String metricName : metricNames) {
                Metric metric = metricRegistry.getMetrics().get(metricName);
                try {
                    if (metric instanceof Gauge) {
                        Number value = (Number) ((Gauge<?>) metric).getValue();
                        if (value instanceof Double) {
                            if (Double.isNaN((Double) value))
                                continue; // Skip these
                        }
                        metricsBuilder.add(metricName, Json.createValue(value));
                    } else if (metric instanceof Meter) {
                        Meter meter = (Meter) metric;
                        JsonObjectBuilder builder = Json.createObjectBuilder();
                        builder.add("count", meter.getCount());
                        builder.add("meanrate", meter.getMeanRate());
                        metricsBuilder.add(metricName, builder.build());
                    } else if (metric instanceof Counter) {
                        Counter counter = (Counter) metric;
                        metricsBuilder.add(metricName, counter.getCount());
                    } else {
//                        System.out.println(metric.getClass());
                    }
                } catch (Throwable e) {
                    LogManager.getLogger(getClass()).error("Could not serialize metric " + metricName + "with value " + metric.toString(), e);
                }
            }
            JsonObject jsonObject = metricsBuilder.build();

            try {
                subscriber.whenComplete((es, t)->
                {
                    es.publishEvent((e, seq, metric) ->
                    {
                        e.metadata.clear();
                        e.event = metric;
                    }, jsonObject);
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void cancel() {

        }
    }
}
