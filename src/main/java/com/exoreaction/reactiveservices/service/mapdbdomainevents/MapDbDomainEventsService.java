package com.exoreaction.reactiveservices.service.mapdbdomainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor.MapDbDomainEventEventHandler;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
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

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class MapDbDomainEventsService
        implements Closeable, ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "mapdbdomainevents";
    public static final Marker MARKER = MarkerManager.getMarker("service:"+SERVICE_TYPE);

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {

        }

        @Override
        protected void configure() {
            context.register(MapDbDomainEventsService.class);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());
    private final Registry registry;
    private ReactiveStreams reactiveStreams;
    private final MapDatabaseService mapDatabaseService;
    private ObjectMapper objectMapper;

    @Inject
    public MapDbDomainEventsService(Registry registry, ReactiveStreams reactiveStreams,
                                    MapDatabaseService mapDatabaseService, ObjectMapper objectMapper) {
        this.registry = registry;
        this.reactiveStreams = reactiveStreams;
        this.mapDatabaseService = mapDatabaseService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onStartup(Container container) {
        registry.addRegistryListener(new DomainEventsRegistryListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public void connect(Link domainEventSource) {
        reactiveStreams.subscribe(domainEventSource, new DomainEventsSubscriber(), Collections.emptyMap(), MarkerManager.getMarker(domainEventSource.getHref()));
    }

    @Override
    public void close() throws IOException {
        // TODO Close active sessions
    }

    private class DomainEventsRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("domainevents").ifPresent(MapDbDomainEventsService.this::connect);
        }
    }

    private class DomainEventsSubscriber
        implements ReactiveEventStreams.Subscriber<EventWithResult<JsonObject, Metadata>>
    {

        private Disruptor<Event<EventWithResult<JsonObject, Metadata>>> disruptor;

        @Override
        public EventSink<Event<EventWithResult<JsonObject, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("MapDbDomainEventsDisruptorIn-"),
                    ProducerType.SINGLE,
                    new BlockingWaitStrategy());
            disruptor.handleEventsWith(new MapDbDomainEventEventHandler(mapDatabaseService, subscription, objectMapper));
            disruptor.start();
            subscription.request(1);
            return disruptor.getRingBuffer();
        }

        @Override
        public void onComplete() {
            disruptor.shutdown();
        }
    }
}
