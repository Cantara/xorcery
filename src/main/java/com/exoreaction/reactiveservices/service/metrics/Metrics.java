package com.exoreaction.reactiveservices.service.metrics;

import com.codahale.metrics.*;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.handlers.JsonObjectSerializerEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.helpers.ServiceResourceObjectBuilder;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceReference;
import com.lmax.disruptor.EventHandler;
import jakarta.inject.Inject;
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

@Singleton
public class Metrics
        implements ContainerLifecycleListener, ReactiveEventStreams.Publisher<JsonObject> {

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return "metrics";
        }

        @Override
        protected void buildResourceObject(ServiceResourceObjectBuilder builder) {
            builder.api("metrics", "api/metrics")
                    .websocket("metricevents", "ws/metricevents", "metrics={metric_names}");
        }

        @Override
        protected void configure() {
//            context.register(MetricsService.class, ContainerLifecycleListener.class);
            bind(resourceObject);
            bind(Metrics.class).to(ContainerLifecycleListener.class);
        }
    }

    private ResourceObject resourceObject;
    private final ReactiveStreams reactiveStreams;
    private final ServiceLinkReference streamReference;
    private MetricRegistry metricRegistry;

    @Inject
    public Metrics(ResourceObject resourceObject, ReactiveStreams reactiveStreams, Server server, MetricRegistry metricRegistry) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        streamReference = new ServiceLinkReference(new ServiceReference("metrics", server.getServerId()), "metricevents");
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void onStartup(Container container) {
        reactiveStreams.publish(streamReference, resourceObject.getLinks().getRel("metricevents").orElseThrow(),
                this, new JsonObjectSerializerEventHandler());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<JsonObject> subscriber, Map<String, String> parameters) {
        String metricNames = parameters.get("metrics");
        Collection<String> metricNamesList = metricNames.isBlank() ? metricRegistry.getNames() : Arrays.asList(metricNames.split(","));

        new MetricSubscription(subscriber, metricNamesList, metricRegistry);
    }

    private class MetricSubscription
            implements ReactiveEventStreams.Subscription {
        private final EventHandler<Event<JsonObject>> handler;
        private Collection<String> metricNames;
        private MetricRegistry metricRegistry;

        public MetricSubscription(ReactiveEventStreams.Subscriber<JsonObject> subscriber, Collection<String> metricNames, MetricRegistry metricRegistry) {
            handler = subscriber.onSubscribe(this);
            this.metricNames = metricNames;
            this.metricRegistry = metricRegistry;
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

            Event<JsonObject> event = new Event<JsonObject>();
            event.metadata = new Metadata();
            event.event = jsonObject;
            try {
                handler.onEvent(event, 1, true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void cancel() {

        }
    }
}
