package com.exoreaction.reactiveservices.service.neo4jdomainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
import com.exoreaction.reactiveservices.service.conductor.api.ConductorListener;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.service.model.ServiceAttributes;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabase;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphDatabases;
import com.exoreaction.reactiveservices.service.neo4jdomainevents.disruptor.Neo4jDomainEventEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
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
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Singleton
public class Neo4jDomainEventsService
        implements ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "neo4jdomainevents";

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
    private ReactiveStreams reactiveStreams;
    private Conductor conductor;
    private ServiceResourceObject sro;
    private GraphDatabases graphDatabases;

    @Inject
    public Neo4jDomainEventsService(Conductor conductor, ReactiveStreams reactiveStreams,
                                    @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                    GraphDatabases graphDatabases) {
        this.conductor = conductor;
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.graphDatabases = graphDatabases;
    }

    @Override
    public void onStartup(Container container) {
        conductor.addConductorListener(new DomainEventsConductorListener());
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    private Consumer<Link> connect(Optional<ServiceAttributes> publisherParameters, Optional<ServiceAttributes> selfParameters) {
        return link ->
        {
            reactiveStreams.subscribe(sro.serviceIdentifier(), link, new DomainEventsSubscriber(selfParameters), publisherParameters);
        };
    }

    private class DomainEventsConductorListener implements ConductorListener {

        @Override
        public void addedGroup(Group group) {
            // Does the added group contain this service?
            if (group.contains(sro)) {
                // Find publisher to connect to
                group.servicesByLinkRel("domainevents", (sro, attributes) ->
                {
                    // Find Neo4j settings
                    Optional<ServiceAttributes> selfAttributes = group.serviceAttributes(Neo4jDomainEventsService.this.sro.serviceIdentifier());
                    sro.linkByRel("domainevents").ifPresent(connect(attributes, selfAttributes));
                });
            }
        }
    }

    private class DomainEventsSubscriber
            implements ReactiveEventStreams.Subscriber<EventWithResult<JsonArray, Metadata>> {

        private Disruptor<Event<EventWithResult<JsonArray, Metadata>>> disruptor;
        private Optional<ServiceAttributes> selfParameters;

        public DomainEventsSubscriber(Optional<ServiceAttributes> selfParameters) {

            this.selfParameters = selfParameters;
        }

        @Override
        public EventSink<Event<EventWithResult<JsonArray, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
            disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Neo4jDomainEventsDisruptorIn-"),
                    ProducerType.SINGLE,
                    new BlockingWaitStrategy());
            String databaseName = selfParameters.flatMap(sa -> sa.attributes().getOptionalString("database"))
                    .orElse("neo4j");
            GraphDatabase graphDatabase = graphDatabases.apply(databaseName);
            disruptor.handleEventsWith(new Neo4jDomainEventEventHandler(graphDatabase.graphDatabaseService(), subscription));
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
