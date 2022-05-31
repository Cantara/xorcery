package com.exoreaction.reactiveservices.service.eventstore;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.ParseError;
import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.eventstore.disruptor.EventStoreDomainEventEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.nio.ByteBuffer;

@Singleton
public class EventStoreService
        implements ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "eventstore";
    private final EventStoreDBClient client;
    private ServiceResourceObject sro;
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
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.websocket("eventstore", "ws/eventstore");
        }

        @Override
        protected void configure() {
            context.register(EventStoreService.class, ContainerLifecycleListener.class);
        }
    }

    @Inject
    public EventStoreService(@Named(SERVICE_TYPE) ServiceResourceObject sro,
                             Configuration configuration, ReactiveStreams reactiveStreams, Registry registry) throws ParseError {
        this.sro = sro;
        this.reactiveStreams = reactiveStreams;
        this.registry = registry;
        String connectionString = configuration.getConfiguration("eventstore").getString("url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        client = EventStoreDBClient.create(settings);
    }

    @Override
    public void onStartup(Container container) {

    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void connect(Link domainEventSource) {
        reactiveStreams.subscribe(sro.serviceIdentifier(), domainEventSource, new EventStoreService.DomainEventsSubscriber());
    }


    private class EventStoreRegistryListener implements RegistryListener {
        @Override
        public void addedService(ServiceResourceObject service) {
            service.getLinkByRel("domainevents").ifPresent(EventStoreService.this::connect);
        }
    }

    private class DomainEventsSubscriber
            implements ReactiveEventStreams.Subscriber<EventWithResult<ByteBuffer, Metadata>> {

        private Disruptor<Event<EventWithResult<ByteBuffer, Metadata>>> disruptor;

        @Override
        public EventSink<Event<EventWithResult<ByteBuffer, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("EventStoreDomainEventsDisruptorIn-"),
                    ProducerType.SINGLE,
                    new BlockingWaitStrategy());
            disruptor.handleEventsWith(new EventStoreDomainEventEventHandler(client, subscription));
            disruptor.start();
            subscription.request(4096);
            return disruptor.getRingBuffer();
        }

        @Override
        public void onComplete() {
            disruptor.shutdown();
        }
    }
}
