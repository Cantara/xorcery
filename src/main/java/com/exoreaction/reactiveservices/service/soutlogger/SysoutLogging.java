package com.exoreaction.reactiveservices.service.soutlogger;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Link;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.service.loggingconsumer.disruptor.Log4jDeserializeEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.exoreaction.reactiveservices.service.registry.client.RegistryClient;
import com.exoreaction.reactiveservices.service.registry.client.RegistryListener;
import com.lmax.disruptor.AggregateEventHandler;
import com.lmax.disruptor.EventHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;
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
    private RegistryClient registryClient;

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
    public SysoutLogging(ReactiveStreams reactiveStreams, RegistryClient registryClient) {
        this.reactiveStreams = reactiveStreams;
        this.registryClient = registryClient;
    }

    @Override
    public void onStartup(Container container) {
        System.out.println("Sysoutlogger Startup");

        registryClient.addRegistryListener(new LoggingRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

        // TODO Close active sessions
        System.out.println("Shutdown");
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
        public EventHandler<Event<LogEvent>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
            return new AggregateEventHandler<>(new Log4jDeserializeEventHandler(new JsonLogEventParser()), this);
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
