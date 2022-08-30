package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.AbstractConductorListener;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.service.eventstore.disruptor.EventStoreDomainEventEventHandler;
import com.exoreaction.xorcery.service.eventstore.model.StreamModel;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;
import org.glassfish.jersey.spi.Contract;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.*;

@Contract
@Singleton
public class EventStoreService
        implements
        ReactiveEventStreams.Publisher<ByteBuffer>,
        ContainerLifecycleListener {

    private static final Logger logger = LogManager.getLogger(EventStoreService.class);

    public static final String SERVICE_TYPE = "eventstore";
    private final EventStoreDBClient client;
    private final ObjectMapper objectMapper;
    private ServiceResourceObject sro;
    private ReactiveStreams reactiveStreams;
    private Conductor conductor;

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("eventstore", "api/eventstore");
            builder.websocket("events", "ws/events");
        }

        @Override
        protected void configure() {
            context.register(EventStoreService.class, EventStoreService.class, ContainerLifecycleListener.class);
        }
    }

    @Inject
    public EventStoreService(@Named(SERVICE_TYPE) ServiceResourceObject sro,
                             Configuration configuration, ReactiveStreams reactiveStreams,
                             Conductor conductor) throws ParseError {
        this.sro = sro;
        this.reactiveStreams = reactiveStreams;
        this.conductor = conductor;
        this.objectMapper = new ObjectMapper();
        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        client = EventStoreDBClient.create(settings);

        // Test connection
        StreamMetadata metadata = client.getStreamMetadata("$all").join();
        logger.info("$all stream metadata:"+metadata.toString());
    }

    @Override
    public void onStartup(Container container) {
        // Read
        sro.getLinkByRel("events").ifPresent(link ->
        {
            reactiveStreams.publisher(sro.serviceIdentifier(), link, this);
        });

        // Write
        conductor.addConductorListener(new DomainEventsConductorListener(sro.serviceIdentifier(), "domainevents"));
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    public CompletionStage<StreamModel> getStream(String id)
    {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, 1, ReadStreamOptions.get().backwards().fromEnd()),
                (streamMetaData, readResult)->
                {

                    return new StreamModel(id, readResult.getEvents().stream().findFirst().map(event -> event.getEvent().getStreamRevision().getValueUnsigned()).orElse(-1L));
                });
    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<ByteBuffer> subscriber, Configuration configuration) {

        try {
            EventStoreParameters parameters = objectMapper.treeToValue(configuration.json(), EventStoreParameters.class);

            DomainEventsProcessor processor = new DomainEventsProcessor(subscriber);
            processor.setSink(subscriber.onSubscribe(processor, Configuration.empty()));
            SubscribeToStreamOptions subscribeToStreamOptions = parameters.from == 0 ?
                    SubscribeToStreamOptions.get().fromStart() :
                    SubscribeToStreamOptions.get().fromRevision(parameters.from);
            processor.setSubscription(client.subscribeToStream(parameters.stream, processor, subscribeToStreamOptions).get());
        } catch (Throwable e) {
            subscriber.onError(e);
            subscriber.onComplete();
        }
    }

    public void connect(Link domainEventSource) {
        reactiveStreams.subscribe(sro.serviceIdentifier(), domainEventSource, new EventStoreService.DomainEventsSubscriber(), Configuration.empty(), Configuration.empty());
    }

    private class DomainEventsConductorListener extends AbstractConductorListener {

        public DomainEventsConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
            super(serviceIdentifier, "domainevents");
        }

        @Override
        public void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration) {
            reactiveStreams.subscribe(this.serviceIdentifier, link, new DomainEventsSubscriber(), sourceConfiguration, Configuration.empty());
        }
    }

    private class DomainEventsSubscriber
            implements ReactiveEventStreams.Subscriber<EventWithResult<ByteBuffer, Metadata>> {

        private Disruptor<Event<EventWithResult<ByteBuffer, Metadata>>> disruptor;

        @Override
        public EventSink<Event<EventWithResult<ByteBuffer, Metadata>>> onSubscribe(ReactiveEventStreams.Subscription subscription, Configuration configuration) {
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

    private class DomainEventsProcessor
            extends SubscriptionListener
            implements ReactiveEventStreams.Subscription {

        public DomainEventsProcessor(ReactiveEventStreams.Subscriber<ByteBuffer> subscriber) {
            this.subscriber = subscriber;
        }

        private ReactiveEventStreams.Subscriber<ByteBuffer> subscriber;
        private EventSink<Event<ByteBuffer>> sink;
        private com.eventstore.dbclient.Subscription subscription;

        @Override
        public void request(long n) {
            // TODO Semaphore this one
        }

        @Override
        public void cancel() {
            subscription.stop();
        }

        public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent resolvedEvent) {
            sink.publishEvent((event, seq, e) ->
            {
                try {
                    logger.info(MarkerManager.getMarker(sro.serviceIdentifier().toString()), "Read metadata: " + new String(e.getEvent().getUserMetadata()));
                    logger.info(MarkerManager.getMarker(sro.serviceIdentifier().toString()), "Read event: " + new String(e.getEvent().getEventData()));
                    event.metadata = new Metadata((ObjectNode) objectMapper.readTree(e.getEvent().getUserMetadata()));
                    event.event = ByteBuffer.wrap(e.getEvent().getEventData());

                    // Put in ES metadata
                    new EventStoreMetadata.Builder(event.metadata.toBuilder())
                            .streamId(e.getEvent().getStreamId())
                            .revision(e.getEvent().getStreamRevision().getValueUnsigned())
                            .contentType(e.getEvent().getContentType());
                } catch (IOException ex) {
                    subscriber.onError(ex);
                }
            }, resolvedEvent);
        }

        @Override
        public void onError(com.eventstore.dbclient.Subscription subscription, Throwable throwable) {
            super.onError(subscription, throwable);
        }

        @Override
        public void onCancelled(com.eventstore.dbclient.Subscription subscription) {
            super.onCancelled(subscription);
        }

        public void setSink(EventSink<Event<ByteBuffer>> onSubscribe) {
            this.sink = onSubscribe;
        }

        public void setSubscription(com.eventstore.dbclient.Subscription subscription) {
            this.subscription = subscription;
        }
    }
}
