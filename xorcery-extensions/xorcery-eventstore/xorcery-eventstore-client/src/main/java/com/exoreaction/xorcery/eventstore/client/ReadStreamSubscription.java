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
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class ReadStreamSubscription
        extends SubscriptionListener {

    private static final JsonMapper jsonMapper = new JsonMapper();

    private final EventStoreDBClient client;
    private final FluxSink<MetadataByteBuffer> sink;
    private final Logger logger;
    volatile CompletableFuture<com.eventstore.dbclient.Subscription> subscribeFuture;

    final AtomicBoolean caughtUp = new AtomicBoolean();
    final AtomicBoolean fellBehind = new AtomicBoolean();
    final AtomicLong position = new AtomicLong();
    final AtomicLong outstandingRequests = new AtomicLong();

    private Marker marker;

    String streamId;
    boolean keepAlive;

    public ReadStreamSubscription(EventStoreDBClient client, FluxSink<MetadataByteBuffer> sink, Logger logger) {
        this.client = client;
        this.sink = sink;
        this.logger = logger;

        try {
            ContextViewElement contextElement = new ContextViewElement(sink.contextView());
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

            sink.onRequest(this::release);
            sink.onCancel(this::cancel);
        } catch (IllegalArgumentException e) {
            sink.error(e);
        }
    }

    synchronized void release(long r) {
        outstandingRequests.addAndGet(r);
        this.notifyAll();
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
            sink.error(throwable);
    }

    public void cancel() {
        subscribeFuture.whenComplete(this::stopSubscription);
    }

    private void stopSubscription(com.eventstore.dbclient.Subscription subscription, Throwable throwable) {
        if (throwable == null)
            subscription.stop();
    }

    @Override
    synchronized public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent resolvedEvent) {
        try {
            RecordedEvent event = resolvedEvent.getEvent();
            if (event == null || event.getEventType().equals("$metadata")) {
                // Event has been deleted or is not relevant
                return;
            }

            if (outstandingRequests.get() == 0)
            {
                System.out.println("Wait for requests");
            }

            while (!sink.isCancelled() && outstandingRequests.get() == 0) {
                // Loop until we get a request
                try {
                    this.wait(5000);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            if (sink.isCancelled())
                return;

            byte[] userMetadata = resolvedEvent.getEvent().getUserMetadata();
            if (userMetadata.length == 0) {
                logger.warn(marker, "Event has no user metadata. Event: " + new String(resolvedEvent.getEvent().getEventData()));
                return;
            }

            outstandingRequests.decrementAndGet();

            if (logger.isTraceEnabled())
                logger.trace(marker, "Read metadata: " + new String(userMetadata));

            CompletableFuture.runAsync(()->
            {
                try {
                    Metadata.Builder metadata = new Metadata.Builder((ObjectNode) jsonMapper.readTree(userMetadata));

                    // Put in ES metadata
                    RecordedEvent linkedEvent = resolvedEvent.getLink();
                    long position;
                    if (linkedEvent != null) {
                        String streamId = linkedEvent.getStreamId();
                        position = linkedEvent.getRevision();
                        String originalStreamId = resolvedEvent.getEvent().getStreamId();
                        metadata.add(EventStoreMetadata.streamId, streamId);
                        metadata.add(EventStoreMetadata.streamPosition, position);
                        metadata.add(EventStoreMetadata.originalStreamId, originalStreamId);
                    } else {
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
                    sink.next(new MetadataByteBuffer(metadata.build(), ByteBuffer.wrap(event.getEventData())));
                } catch (IOException e) {
                    cancel();
                    sink.error(e);
                }
            });
        } catch (Throwable e) {
            cancel();
            sink.error(e);
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
        } else if (throwable != null) {
            sink.error(throwable);
        }
    }

    @Override
    public void onCaughtUp(com.eventstore.dbclient.Subscription subscription) {
        if (keepAlive) {
            caughtUp.set(true);
            fellBehind.set(false);
//            System.out.println("Caught up");
        } else {
            sink.complete();
            subscription.stop();
//            System.out.println("Complete");
        }
    }

    @Override
    public void onFellBehind(com.eventstore.dbclient.Subscription subscription) {
        fellBehind.set(true);
        caughtUp.set(false);
    }
}
