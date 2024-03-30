package com.exoreaction.xorcery.eventstore.reactor;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.BaseSubscriber;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

public class ReadStream
        implements Publisher<MetadataByteBuffer> {

    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final String streamName;
    private final Logger logger;
    private final Marker marker;

    public ReadStream(EventStoreDBClient client, String streamName, Logger logger) {
        this.client = client;
        this.streamName = streamName;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(streamName);
    }

    @Override
    public void subscribe(Subscriber<? super MetadataByteBuffer> subscriber) {
        subscriber.onSubscribe(new ReadStreamSubscription(subscriber));
    }

    private class ReadStreamSubscription
            extends BaseSubscriber<ReadMessage>
            implements Subscription {
        Subscriber<? super MetadataByteBuffer> subscriber;

        public ReadStreamSubscription(Subscriber<? super MetadataByteBuffer> subscriber) {
            this.subscriber = subscriber;

            long position = 0;
            if (subscriber instanceof CoreSubscriber<? super MetadataByteBuffer> coreSubscriber) {
                if (coreSubscriber.currentContext().getOrDefault("position", null) instanceof Long pos) {
                    position = pos;
                }
            }

            client.readStreamReactive(streamName, ReadStreamOptions.get()
                    .fromRevision(position)
                    .deadline(30000)
                    .resolveLinkTos()
                    .notRequireLeader()).subscribe(this);
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            // Ignore
        }

        @Override
        protected void hookOnNext(ReadMessage value) {

            try {
                if (!value.hasEvent())
                {
                    request(1);
                    return;
                }

                ResolvedEvent resolvedEvent = value.getEvent();
                RecordedEvent event = resolvedEvent.getEvent();
                if (event == null || event.getEventType().equals("$metadata")) {
                    // Event has been deleted or is not relevant
                    request(1);
                    return;
                }

                byte[] userMetadata = event.getUserMetadata();
                if (userMetadata.length == 0) {
                    logger.warn(marker, "Event has no user metadata. Event: " + new String(event.getEventData()));
                    return;
                }
                if (logger.isTraceEnabled())
                    logger.trace(marker, "Read metadata: " + new String(userMetadata));

                Metadata.Builder metadata = new Metadata.Builder((ObjectNode) jsonMapper.readTree(userMetadata));

                // Put in ES metadata
                String streamId = resolvedEvent.getLink() != null ? resolvedEvent.getLink().getStreamId() : resolvedEvent.getEvent().getStreamId();
                long position = resolvedEvent.getLink() != null ? resolvedEvent.getLink().getRevision() : resolvedEvent.getEvent().getRevision();
                metadata.add("streamId", streamId);
                metadata.add("position", position);
                subscriber.onNext(new MetadataByteBuffer(metadata.build(), ByteBuffer.wrap(event.getEventData())));
            } catch (IOException e) {
                subscriber.onError(e);
            }
        }

        @Override
        protected void hookOnComplete() {
            subscriber.onComplete();
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            subscriber.onError(throwable);
        }
    }
}
