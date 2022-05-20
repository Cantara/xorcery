package com.exoreaction.reactiveservices.service.neo4jdomainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.neo4jdomainevents.disruptor.Neo4jDomainEventEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.exoreaction.reactiveservices.service.registry.api.RegistryListener;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.JsonArray;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collections;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class Neo4jDomainEventsService
        implements ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "neo4jdomainevents";
    public static final Marker MARKER = MarkerManager.getMarker("service:" + SERVICE_TYPE);

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
            context.register(Neo4jDomainEventsService.class);
        }
    }

    private final Logger logger = LogManager.getLogger(getClass());
    private final Registry registry;
    private ReactiveStreams reactiveStreams;
    private ServiceResourceObject sro;
    private GraphDatabaseService graphDatabaseService;
    private ObjectMapper objectMapper;

    @Inject
    public Neo4jDomainEventsService(Registry registry, ReactiveStreams reactiveStreams,
                                    @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                    GraphDatabaseService graphDatabaseService) {
        this.registry = registry;
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.graphDatabaseService = graphDatabaseService;
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
        reactiveStreams.subscribe(sro.serviceIdentifier(), domainEventSource, new DomainEventsSubscriber());
    }

    private class DomainEventsRegistryListener implements RegistryListener {
        @Override
        public void addedService(ResourceObject service) {
            service.getLinks().getRel("domainevents").ifPresent(Neo4jDomainEventsService.this::connect);
        }
    }

    private class DomainEventsSubscriber
            implements ReactiveEventStreams.Subscriber<EventWithResult<JsonArray, Metadata>> {

        private Disruptor<Event<EventWithResult<JsonArray, Metadata>>> disruptor;

        @Override
        public EventSink<Event<EventWithResult<JsonArray, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Neo4jDomainEventsDisruptorIn-"),
                    ProducerType.SINGLE,
                    new BlockingWaitStrategy());
            disruptor.handleEventsWith(new Neo4jDomainEventEventHandler(graphDatabaseService, subscription));
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
