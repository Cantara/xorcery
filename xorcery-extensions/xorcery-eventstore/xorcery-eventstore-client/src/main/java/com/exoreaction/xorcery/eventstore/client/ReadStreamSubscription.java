package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreContext;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ReactiveStreamsContext;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.grpc.StatusRuntimeException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ReadStreamSubscription
        implements Publisher<MetadataByteBuffer> {

    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final Logger logger;

    public ReadStreamSubscription(EventStoreDBClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataByteBuffer> subscriber) {
        subscriber.onSubscribe(new ReadStreamSubscriptionListener(subscriber));
    }

    private class ReadStreamSubscriptionListener
            extends SubscriptionListener
            implements Subscription {
        final Subscriber<? super MetadataByteBuffer> subscriber;
        final Semaphore outstandingRequests = new Semaphore(0);
        volatile CompletableFuture<com.eventstore.dbclient.Subscription> subscribeFuture;
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean caughtUp = new AtomicBoolean();
        final AtomicBoolean fellBehind = new AtomicBoolean();
        final AtomicLong position = new AtomicLong();

        final long maxRequests = Integer.MAX_VALUE;
        private Marker marker;

        String streamId;
        boolean keepAlive;

        public ReadStreamSubscriptionListener(Subscriber<? super MetadataByteBuffer> subscriber) {
            this.subscriber = subscriber;

            try {
                if (subscriber instanceof CoreSubscriber<? super MetadataByteBuffer> coreSubscriber) {
                    ContextViewElement contextElement = new ContextViewElement(coreSubscriber.currentContext());
                    this.streamId = contextElement.getString(ReactiveStreamsContext.streamId).orElseThrow(() -> new IllegalArgumentException("Missing 'streamId' context parameter"));
                    this.marker = MarkerManager.getMarker(streamId);
                    this.keepAlive = contextElement.getBoolean(EventStoreContext.keepAlive).orElse(false);

                    contextElement.getLong(ReactiveStreamsContext.streamPosition)
                            .map(pos ->
                            {
                                position.set(pos);
                                return pos;
                            }).ifPresentOrElse(pos -> start(StreamPosition.position(pos)),
                                    () -> start(StreamPosition.start()));
                } else {
                    throw new IllegalArgumentException("Subscriber must implement CoreSubscriber");
                }
            } catch (IllegalArgumentException e) {
                subscriber.onError(e);
            }
        }

        public void start(StreamPosition<Long> streamPosition) {
            subscribeFuture = client.subscribeToStream(streamId, this, SubscribeToStreamOptions.get()
                    .fromRevision(streamPosition)
                    .deadline(30000)
                    .resolveLinkTos()
                    .notRequireLeader()
            ).whenComplete(this::subscribed);
        }

        private void subscribed(com.eventstore.dbclient.Subscription subscription, Throwable throwable) {
            if (throwable != null)
                subscriber.onError(throwable);
        }

        @Override
        public void request(long n) {
            outstandingRequests.release((int) Math.min(n, maxRequests));
        }

        @Override
        public void cancel() {
            subscribeFuture.whenComplete(this::stopSubscription).thenRun(() -> cancelled.set(true));
        }

        private void stopSubscription(com.eventstore.dbclient.Subscription subscription, Throwable throwable) {
            if (throwable == null)
                subscription.stop();
        }

        @Override
        public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent resolvedEvent) {
            try {
                RecordedEvent event = resolvedEvent.getEvent();
                if (event == null || event.getEventType().equals("$metadata")) {
                    // Event has been deleted or is not relevant
                    return;
                }

                while (!cancelled.get() && !outstandingRequests.tryAcquire(5, TimeUnit.SECONDS)) {
                    // Loop until we get a request
                    if (cancelled.get())
                        return;
                }

                byte[] userMetadata = resolvedEvent.getEvent().getUserMetadata();
                if (userMetadata.length == 0) {
                    logger.warn(marker, "Event has no user metadata. Event: " + new String(resolvedEvent.getEvent().getEventData()));
                    outstandingRequests.release();
                    return;
                }
                if (logger.isTraceEnabled())
                    logger.trace(marker, "Read metadata: " + new String(userMetadata));

                Metadata.Builder metadata = new Metadata.Builder((ObjectNode) jsonMapper.readTree(userMetadata));

                // Put in ES metadata
                RecordedEvent linkedEvent = resolvedEvent.getLink();
                long position;
                if (linkedEvent != null)
                {
                    String streamId = linkedEvent.getStreamId();
                    position = linkedEvent.getRevision();
                    String originalStreamId = resolvedEvent.getEvent().getStreamId();
                    metadata.add(EventStoreMetadata.streamId, streamId);
                    metadata.add(EventStoreMetadata.streamPosition, position);
                    metadata.add(EventStoreMetadata.originalStreamId, originalStreamId);
                } else
                {
                    RecordedEvent recordedEvent = resolvedEvent.getEvent();
                    String streamId = recordedEvent.getStreamId();
                    position = recordedEvent.getRevision();
                    metadata.add(EventStoreMetadata.streamId, streamId);
                    metadata.add(EventStoreMetadata.streamPosition, position);
                }

                if (caughtUp.getAndSet(false)) {
                    metadata.add(EventStoreMetadata.streamLive, JsonNodeFactory.instance.booleanNode(true));
                } else if (fellBehind.getAndSet(false)) {
                    metadata.add(EventStoreMetadata.streamLive, JsonNodeFactory.instance.booleanNode(false));
                }
                this.position.set(position);
                subscriber.onNext(new MetadataByteBuffer(metadata.build(), ByteBuffer.wrap(event.getEventData())));

            } catch (Throwable e) {
                cancel();
                subscriber.onError(e);
            }
        }

        @Override
        public void onCancelled(com.eventstore.dbclient.Subscription subscription, Throwable throwable) {
            if (throwable instanceof StatusRuntimeException sre) {
                switch ((sre.getStatus().getCode())) {
                    case ABORTED: {
                        logger.warn("EventStore aborted, reconnecting", throwable);
                        start(StreamPosition.position(position.get()));
                        return;
                    }

                    default: {
                        logger.error("EventStore error", throwable);
                    }
                }
            } else if (throwable instanceof NotLeaderException) {
                // Simply retry
                start(StreamPosition.position(position.get()));
                return;
            }

            if (subscriber != null) {
                if (throwable != null)
                    subscriber.onError(throwable);
            }
        }

        @Override
        public void onCaughtUp(com.eventstore.dbclient.Subscription subscription) {
            if (keepAlive)
            {
                caughtUp.set(true);
                fellBehind.set(false);
                System.out.println("Caught up");
            } else
            {
                subscriber.onComplete();
                subscription.stop();
                System.out.println("Complete");
            }
        }

        @Override
        public void onFellBehind(com.eventstore.dbclient.Subscription subscription) {
            fellBehind.set(true);
            caughtUp.set(false);
        }
    }
}
