package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.SubscribeToStreamOptions;
import com.eventstore.dbclient.SubscriptionListener;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;

public class EventStorePublisher
        implements Flow.Publisher<WithMetadata<ByteBuffer>> {
    private static final Logger logger = LogManager.getLogger(EventStorePublisher.class);

    private final EventStoreDBClient client;
    private final ObjectMapper objectMapper;
    private Configuration publisherConfiguration;

    public EventStorePublisher(EventStoreDBClient client, ObjectMapper objectMapper, Configuration publisherConfiguration) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ByteBuffer>> subscriber) {
        new DomainEventsProcessor(subscriber);
    }

    private class DomainEventsProcessor
            extends SubscriptionListener
            implements Flow.Subscription {

        private Flow.Subscriber<? super WithMetadata<ByteBuffer>> subscriber;
        private com.eventstore.dbclient.Subscription subscription;

        public DomainEventsProcessor(Flow.Subscriber<? super WithMetadata<ByteBuffer>> subscriber) {
            try {
                this.subscriber = subscriber;

                EventStoreParameters parameters = objectMapper.treeToValue(publisherConfiguration.json(), EventStoreParameters.class);

                subscriber.onSubscribe(this);

                SubscribeToStreamOptions subscribeToStreamOptions = parameters.from == 0 ?
                        SubscribeToStreamOptions.get().fromStart() :
                        SubscribeToStreamOptions.get().fromRevision(parameters.from);
                subscription = client.subscribeToStream(parameters.stream, this, subscribeToStreamOptions).get();
            } catch (JsonProcessingException | InterruptedException | ExecutionException e) {
                subscriber.onError(e);
            }
        }

        @Override
        public void request(long n) {
            // TODO Semaphore this one
        }

        @Override
        public void cancel() {
            subscription.stop();
        }

        public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent resolvedEvent) {
            try {
/*
                    logger.info(MarkerManager.getMarker(sro.serviceIdentifier().toString()), "Read metadata: " + new String(e.getEvent().getUserMetadata()));
                    logger.info(MarkerManager.getMarker(sro.serviceIdentifier().toString()), "Read event: " + new String(e.getEvent().getEventData()));
*/

                Metadata.Builder metadata = new Metadata.Builder((ObjectNode) objectMapper.readTree(resolvedEvent.getEvent().getUserMetadata()));

                // Put in ES metadata
                new EventStoreMetadata.Builder(metadata)
                        .streamId(resolvedEvent.getEvent().getStreamId())
                        .revision(resolvedEvent.getEvent().getStreamRevision().getValueUnsigned())
                        .contentType(resolvedEvent.getEvent().getContentType());

                subscriber.onNext(new WithMetadata<>(metadata.build(), ByteBuffer.wrap(resolvedEvent.getEvent().getEventData())));

            } catch (IOException ex) {
                subscriber.onError(ex);
                subscription.stop();
            }
        }

        @Override
        public void onError(com.eventstore.dbclient.Subscription subscription, Throwable throwable) {
            subscriber.onError(throwable);
            subscription.stop();
        }

        @Override
        public void onCancelled(com.eventstore.dbclient.Subscription subscription) {
            subscriber.onComplete();
        }
    }
}
