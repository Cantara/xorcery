package com.exoreaction.reactiveservices.service.soutmetrics;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.conductor.api.AbstractConductorListener;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
import com.exoreaction.reactiveservices.server.model.ServiceAttributes;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class SysoutMetrics
        implements ContainerLifecycleListener {
    public static final String SERVICE_TYPE = "soutmetrics";

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
    private Conductor conductor;
    private ServiceResourceObject sro;

    @Inject
    public SysoutMetrics(ReactiveStreams reactiveStreams, Conductor conductor, @Named(SERVICE_TYPE) ServiceResourceObject sro) {
        this.reactiveStreams = reactiveStreams;
        this.conductor = conductor;
        this.sro = sro;
    }

    @Override
    public void onStartup(Container container) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        conductor.addConductorListener(new MetricsConductorListener(sro.serviceIdentifier(), "metricevents"));
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        scheduledExecutorService.shutdown();
    }

    private static class MetricEventSubscriber
            implements ReactiveEventStreams.Subscriber<JsonObject>, EventHandler<Event<JsonObject>> {
        private final ScheduledExecutorService scheduledExecutorService;
        private ReactiveEventStreams.Subscription subscription;
        private final long delay;

        public MetricEventSubscriber(Optional<ServiceAttributes> selfParameters, ScheduledExecutorService scheduledExecutorService) {

            this.scheduledExecutorService = scheduledExecutorService;
            this.delay = Duration.parse(selfParameters.flatMap(sa -> sa.attributes().getOptionalString("delay")).orElse("5S")).toSeconds();
        }

        @Override
        public EventSink<Event<JsonObject>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;

            Disruptor<Event<JsonObject>> disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("SysoutMetrics-"));
            disruptor.handleEventsWith(this);
            disruptor.start();

            scheduledExecutorService.schedule(() -> subscription.request(1), delay, TimeUnit.SECONDS);

            return disruptor.getRingBuffer();
        }

        @Override
        public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("Metric:" + event.event.toString() + ":" + event.metadata);
            scheduledExecutorService.schedule(() -> subscription.request(1), delay, TimeUnit.SECONDS);
        }
    }

    private class MetricsConductorListener extends AbstractConductorListener {

        public MetricsConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
            super(serviceIdentifier, rel);
        }

        public void connect(ServiceResourceObject sro, Link link, Optional<ServiceAttributes> attributes, Optional<ServiceAttributes> selfAttributes) {
            reactiveStreams.subscribe(serviceIdentifier, link,
                    new MetricEventSubscriber(selfAttributes, scheduledExecutorService), attributes);
        }
    }
}
