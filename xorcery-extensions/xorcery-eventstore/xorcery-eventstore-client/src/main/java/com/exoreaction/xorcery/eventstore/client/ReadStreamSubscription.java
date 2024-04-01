package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreContext;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
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
import reactor.util.context.Context;

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

        public ReadStreamSubscriptionListener(Subscriber<? super MetadataByteBuffer> subscriber) {
            this.subscriber = subscriber;

            if (subscriber instanceof CoreSubscriber<? super MetadataByteBuffer> coreSubscriber) {
                Context context = coreSubscriber.currentContext();
                streamId = context.get(EventStoreContext.streamId.name());
                this.marker = MarkerManager.getMarker(streamId);

                if (context.getOrDefault(EventStoreContext.streamPosition.name(), null) instanceof Long pos) {
                    position.set(pos);
                    start(StreamPosition.position(pos));
                } else
                {
                    start(StreamPosition.start());
                }
            } else
            {
                subscriber.onError(new IllegalArgumentException("Subscriber must implement CoreSubscriber"));
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
            if (throwable != null)
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
                String streamId = resolvedEvent.getLink() != null ? resolvedEvent.getLink().getStreamId() : resolvedEvent.getEvent().getStreamId();
                long position = resolvedEvent.getLink() != null ? resolvedEvent.getLink().getRevision() : resolvedEvent.getEvent().getRevision();
                metadata.add(EventStoreContext.streamId.name(), streamId);
                metadata.add(EventStoreContext.streamPosition.name(), position);

                if (caughtUp.getAndSet(false)) {
                    metadata.add(EventStoreContext.streamLive.name(), JsonNodeFactory.instance.booleanNode(true));
                } else if (fellBehind.getAndSet(false)) {
                    metadata.add(EventStoreContext.streamLive.name(), JsonNodeFactory.instance.booleanNode(false));
                }

                System.out.println("Position:" + position);
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
            caughtUp.set(true);
            System.out.println("Caught up");
        }

        @Override
        public void onFellBehind(com.eventstore.dbclient.Subscription subscription) {
            fellBehind.set(true);
        }
    }
}
