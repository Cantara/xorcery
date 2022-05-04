package com.exoreaction.reactiveservices.service.soutlogger;

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

    private ReactiveStreams reactiveStreams;
    private Registry registry;

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
    public SysoutLogging(ReactiveStreams reactiveStreams, Registry registry) {
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
    }

    @Override
    public void onStartup(Container container) {
        registry.addRegistryListener(new LoggingRegistryListener());

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
                    new LogEventSubscriber());
        };
    }
    private static class LogEventSubscriber
            implements ReactiveEventStreams.Subscriber<LogEvent>, EventHandler<Event<LogEvent>>
    {
        private ReactiveEventStreams.Subscription subscription;
        @Override
        public EventSink<Event<LogEvent>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;

            Disruptor<Event<LogEvent>> disruptor = new Disruptor<>(Event::new, 1024, new NamedThreadFactory("SysoutLogger-") );
            disruptor.handleEventsWith(this);
            disruptor.start();

            subscription.request(1);

            return disruptor.getRingBuffer();
        }

        @Override
        public void onEvent(Event<LogEvent> event, long sequence, boolean endOfBatch) throws Exception {
            System.out.println("Log:"+event.event.toString()+":"+event.metadata);
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
