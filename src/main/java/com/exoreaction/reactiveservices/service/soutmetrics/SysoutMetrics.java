package com.exoreaction.reactiveservices.service.soutmetrics;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class SysoutMetrics
        implements ContainerLifecycleListener {
    public static final String SERVICE_TYPE = "soutmetrics";
    public static final Marker MARKER = MarkerManager.getMarker("service:"+SERVICE_TYPE);

    private final Logger logger = LogManager.getLogger(getClass());
    private ScheduledExecutorService scheduledExecutorService;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void configure() {
            context.register(SysoutMetrics.class, ContainerLifecycleListener.class);
        }
    }

    private ReactiveStreams reactiveStreams;
    private Registry registry;

    @Inject
    public SysoutMetrics(ReactiveStreams reactiveStreams, Registry registry) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
    }

    @Override
    public void onStartup(Container container) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        registry.addRegistryListener(new MetricsRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        scheduledExecutorService.shutdown();
    }

    @NotNull
    private Consumer<Link> connect(ResourceObject service) {
        return l ->
        {
            reactiveStreams.subscribe(new ServiceLinkReference(service, "metricevents"),
                    new MetricEventSubscriber(scheduledExecutorService));
        };
    }


    private static class MetricEventSubscriber
            implements ReactiveEventStreams.Subscriber<JsonObject>, EventHandler<Event<JsonObject>>
    {
        private final ScheduledExecutorService scheduledExecutorService;
        private ReactiveEventStreams.Subscription subscription;

        public MetricEventSubscriber(ScheduledExecutorService scheduledExecutorService) {

            this.scheduledExecutorService = scheduledExecutorService;
        }

        @Override
        public EventSink<Event<JsonObject>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;

            Disruptor<Event<JsonObject>> disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("SysoutLogger-") );
            disruptor.handleEventsWith(this);
            disruptor.start();

            subscription.request(1);

            return disruptor.getRingBuffer();
        }

        @Override
        public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("Metric:"+event.event.toString()+":"+event.metadata);
            scheduledExecutorService.schedule(()->subscription.request(1), 5, TimeUnit.SECONDS);
        }
    }

    private class MetricsRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("metricevents").ifPresent(connect(service));
        }
    }
}
