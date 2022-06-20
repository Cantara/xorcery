package com.exoreaction.reactiveservices.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.api.AbstractConductorListener;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
import com.exoreaction.reactiveservices.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.reactiveservices.service.eventstore.disruptor.EventStoreDomainEventEventHandler;
import com.exoreaction.reactiveservices.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Optional;

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
            builder.websocket("eventstorestreams", "ws/eventstorestreams");
        }

        @Override
        protected void configure() {
            context.register(EventStoreService.class, ContainerLifecycleListener.class);
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
        String connectionString = configuration.getConfiguration("eventstore").getString("url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        client = EventStoreDBClient.create(settings);

        // Test connection
        StreamMetadata metadata = client.getStreamMetadata("$all").join();
        logger.info("$all stream metadata:"+metadata.toString());
    }

    @Override
    public void onStartup(Container container) {
        // Read
        sro.getLinkByRel("eventstorestreams").ifPresent(link ->
        {
            reactiveStreams.publish(sro.serviceIdentifier(), link, this);
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

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<ByteBuffer> subscriber, ObjectNode jsonParameters) {

        try {
            EventStoreParameters parameters = objectMapper.treeToValue(jsonParameters, EventStoreParameters.class);

            HashMap<String, Object> customProperties = new HashMap<>();
            customProperties.put("foo", "bar");
            StreamMetadata metadata = new StreamMetadata();
            metadata.setCustomProperties(customProperties);
            LogManager.getLogger(getClass()).info("Setting Stream metadata for {}:{}", parameters.stream, metadata.serialize());
            client.setStreamMetadata(parameters.stream, metadata).get();
            LogManager.getLogger(getClass()).info("Stream metadata for {}:{}", parameters.stream, metadata.serialize());

            DomainEventsProcessor processor = new DomainEventsProcessor(subscriber);
            processor.setSink(subscriber.onSubscribe(processor));
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
        reactiveStreams.subscribe(sro.serviceIdentifier(), domainEventSource, new EventStoreService.DomainEventsSubscriber());
    }

    private class DomainEventsConductorListener extends AbstractConductorListener {

        public DomainEventsConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
            super(serviceIdentifier, "domainevents");
        }

        @Override
        public void connect(ServiceResourceObject sro, Link link, Optional<ObjectNode> sourceAttributes, Optional<ObjectNode> consumerAttributes) {
            reactiveStreams.subscribe(this.serviceIdentifier, link, new DomainEventsSubscriber(), sourceAttributes);
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
