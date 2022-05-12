package com.exoreaction.reactiveservices.service.soutlogger;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.reactivestreams.helper.MultiSubscriber;
import com.exoreaction.reactiveservices.service.reactivestreams.helper.SubscriberProxy;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class SysoutLogging
        implements ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "soutlogging";
    private final Meter meter;

    private ReactiveStreams reactiveStreams;
    private Registry registry;
    private Configuration configuration;
    private MetricRegistry metricRegistry;
    private MultiSubscriber<LogEvent> multiSubscriber;

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


    @Inject
    public SysoutLogging(ReactiveStreams reactiveStreams, Registry registry,
                         Configuration configuration, MetricRegistry metricRegistry) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        meter = metricRegistry.meter("logmeter");
    }

    @Override
    public void onStartup(Container container) {
        registry.addRegistryListener(new LoggingRegistryListener());

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
    private Consumer<Link> connect(ResourceObject service) {
        return l ->
        {
            reactiveStreams.subscribe(new ServiceLinkReference(service, "logevents"),
                    new LogSubscriberProxy(multiSubscriber));
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

        @Override
        public void onEvent(Event<LogEvent> event, long sequence, boolean endOfBatch) throws Exception {
//            System.out.println("Log:"+event.event.toString()+":"+event.metadata);
            meter.mark();
            subscription.request(1);
        }
    }

    private class LoggingRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("logevents").ifPresent(connect(service));
        }
    }
}
