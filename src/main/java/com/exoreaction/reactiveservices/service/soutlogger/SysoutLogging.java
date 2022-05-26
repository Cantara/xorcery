package com.exoreaction.reactiveservices.service.soutlogger;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
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
import com.exoreaction.reactiveservices.service.reactivestreams.helper.MultiSubscriber;
import com.exoreaction.reactiveservices.service.reactivestreams.helper.SubscriberProxy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class SysoutLogging
        implements ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "soutlogging";

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void configure() {
            context.register(SysoutLogging.class, ContainerLifecycleListener.class);
        }
    }

    private final Meter meter;
    private ServiceResourceObject serviceResourceObject;

    private ReactiveStreams reactiveStreams;
    private Conductor conductor;
    private Configuration configuration;
    private MultiSubscriber<LogEvent> multiSubscriber;

    @Inject
    public SysoutLogging(ReactiveStreams reactiveStreams, Conductor conductor,
                         Configuration configuration, MetricRegistry metricRegistry,
                         @Named(SERVICE_TYPE) ServiceResourceObject serviceResourceObject) {
        this.reactiveStreams = reactiveStreams;
        this.conductor = conductor;
        this.configuration = configuration;
        meter = metricRegistry.meter("logmeter");
        this.serviceResourceObject = serviceResourceObject;
    }

    @Override
    public void onStartup(Container container) {
        conductor.addConductorListener(new LoggingConductorListener(serviceResourceObject.serviceIdentifier(), "logevents"));

        Disruptor<Event<LogEvent>> disruptor = new Disruptor<>(Event::new, configuration.getInteger("sysoutlogger.size").orElse(4096), new NamedThreadFactory("SysoutLogger-"));
        LogEventSubscriber subscriber = new LogEventSubscriber(disruptor.getRingBuffer());
        disruptor.handleEventsWith(subscriber);
        disruptor.start();

        multiSubscriber = new MultiSubscriber<>(subscriber);
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    @NotNull
    private Consumer<Link> connect(Optional<ServiceAttributes> attributes) {
        return link ->
        {
            reactiveStreams.subscribe(serviceResourceObject.serviceIdentifier(), link, new LogSubscriberProxy(multiSubscriber), attributes.map(ServiceAttributes::toMap).orElse(Collections.emptyMap()));
        };
    }

    private final static class LogSubscriberProxy
            extends SubscriberProxy<LogEvent>
            implements ReactiveEventStreams.Subscriber<LogEvent> {
        public LogSubscriberProxy(MultiSubscriber<LogEvent> multiSubscriber) {
            super(multiSubscriber);
        }
    }

    private class LogEventSubscriber
            implements ReactiveEventStreams.Subscriber<LogEvent>, EventHandler<Event<LogEvent>> {
        private RingBuffer<Event<LogEvent>> ringBuffer;
        private ReactiveEventStreams.Subscription subscription;

        private LogEventSubscriber(RingBuffer<Event<LogEvent>> ringBuffer) {
            this.ringBuffer = ringBuffer;
        }

        @Override
        public EventSink<Event<LogEvent>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(ringBuffer.getBufferSize());
            return ringBuffer;
        }


        long bs;

        @Override
        public void onBatchStart(long batchSize) {
            bs = batchSize;
        }

        @Override
        public void onEvent(Event<LogEvent> event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("Log:"+event.event.toString()+":"+event.metadata);
            meter.mark();
            if (endOfBatch) {
                subscription.request(bs);
            }
        }
    }

    private class LoggingConductorListener extends AbstractConductorListener {

        public LoggingConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
            super(serviceIdentifier, rel);
        }

        public void connect(ServiceResourceObject sro, Link link, Optional<ServiceAttributes> attributes, Optional<ServiceAttributes> selfAttributes) {
            reactiveStreams.subscribe(serviceIdentifier, link, new LogSubscriberProxy(multiSubscriber), attributes.map(ServiceAttributes::toMap).orElse(Collections.emptyMap()));
        }
    }
}
