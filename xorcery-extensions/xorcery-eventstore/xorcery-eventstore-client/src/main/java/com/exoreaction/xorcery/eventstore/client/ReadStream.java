package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.api.MetadataByteBuffer;
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

public class ReadStream
        implements Publisher<MetadataByteBuffer> {

    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final String streamId;
    private final Logger logger;

    public ReadStream(EventStoreDBClient client, String streamId, Logger logger) {
        this.client = client;
        this.streamId = streamId;
        this.logger = logger;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataByteBuffer> subscriber) {
        subscriber.onSubscribe(new ReadStreamSubscription(subscriber));
    }

    private class ReadStreamSubscription
            extends BaseSubscriber<ReadMessage>
            implements Subscription {
        private final Subscriber<? super MetadataByteBuffer> subscriber;
        private Marker marker;

        public ReadStreamSubscription(Subscriber<? super MetadataByteBuffer> subscriber) {
            this.subscriber = subscriber;

            long position = 0;
            String name = streamId;
            if (subscriber instanceof CoreSubscriber<? super MetadataByteBuffer> coreSubscriber) {
                if (coreSubscriber.currentContext().getOrDefault(EventStoreContext.streamPosition.name(), null) instanceof Long pos) {
                    position = pos;
                }
                if (coreSubscriber.currentContext().getOrDefault(EventStoreContext.streamId.name(), null) instanceof String n) {
                    name = n;
                }
            }

            if (name == null) {
                subscriber.onError(new IllegalArgumentException("No streamId name specified"));
                return;
            }

            this.marker = MarkerManager.getMarker(name);
            client.readStreamReactive(name, ReadStreamOptions.get()
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
                if (!value.hasEvent()) {
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
                RecordedEvent linkedEvent = resolvedEvent.getLink();
                if (linkedEvent != null)
                {
                    String streamId = linkedEvent.getStreamId();
                    long position = linkedEvent.getRevision();
                    String originalStreamId = resolvedEvent.getEvent().getStreamId();
                    metadata.add(EventStoreContext.streamId.name(), streamId);
                    metadata.add(EventStoreContext.streamPosition.name(), position);
                    metadata.add(EventStoreContext.originalStreamId.name(), originalStreamId);
                } else
                {
                    RecordedEvent recordedEvent = resolvedEvent.getEvent();
                    String streamId = recordedEvent.getStreamId();
                    long position = recordedEvent.getRevision();
                    metadata.add(EventStoreContext.streamId.name(), streamId);
                    metadata.add(EventStoreContext.streamPosition.name(), position);
                }
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
