/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.common.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherSubscriptionReactiveStream
        extends ServerReactiveStream
        implements WebSocketListener, Subscriber<Object> {

    protected final Logger logger;
    private final ActiveSubscriptions activeSubscriptions;

    private final String streamName;
    protected volatile Session session;

    private final Function<Configuration, Publisher<Object>> publisherFactory;
    private Configuration publisherConfiguration;
    protected Subscription subscription;

    private AtomicBoolean isComplete = new AtomicBoolean();

    private final BlockingArrayQueue<Object> sendQueue = new BlockingArrayQueue<>(4096, 1024);
    private final AtomicBoolean isDraining = new AtomicBoolean();
    private final Lock drainLock = new ReentrantLock();

    private final ByteBufferOutputStream2 outputStream;

    protected final MessageWriter<Object> messageWriter;

    private final ObjectMapper objectMapper;
    protected final ByteBufferPool pool;
    protected final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private long outstandingRequestAmount;
    private ActiveSubscriptions.ActiveSubscription activeSubscription;

    private final LongHistogram sentBytes;
    private final LongHistogram requestsHistogram;
    private final LongHistogram flushHistogram;

    public PublisherSubscriptionReactiveStream(String streamName,
                                               Function<Configuration, Publisher<Object>> publisherFactory,
                                               MessageWriter<Object> messageWriter,
                                               ObjectMapper objectMapper,
                                               ByteBufferPool pool,
                                               Logger logger,
                                               ActiveSubscriptions activeSubscriptions,
                                               OpenTelemetry openTelemetry) {
        this.streamName = streamName;
        this.publisherFactory = publisherFactory;
        this.messageWriter = messageWriter;
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(streamName);

        outputStream = new ByteBufferOutputStream2(pool, false);
        this.logger = logger;
        this.activeSubscriptions = activeSubscriptions;

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        this.attributes = Attributes.builder()
                .put("reactivestream.name", streamName)
                .build();
        this.sentBytes = meter.histogramBuilder("reactivestream.publisher.io")
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder("reactivestream.publisher.requests")
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder("reactivestream.publisher.flush.count")
                .setUnit("{item}").ofLongs().build();
    }

    // Subscriber
    @Override
    public void onSubscribe(Subscription subscription) {

        if (logger.isTraceEnabled())
            logger.trace(marker, "onSubscribe");

        this.subscription = subscription;

        if (outstandingRequestAmount > 0) {
            subscription.request(outstandingRequestAmount);
            outstandingRequestAmount = 0;
        }
    }

    @Override
    public void onNext(Object item) {
//        if (logger.isTraceEnabled())
//            logger.trace(marker, "onNext {}", item.toString());

        if (!sendQueue.offer(item)) {
            logger.error(marker, "Could not put item on queue {}/{}", sendQueue.getCapacity(), sendQueue.getMaxCapacity());
        }

        if (!isDraining.get()) {
            CompletableFuture.runAsync(this::drainJob);
        }
        activeSubscription.requested().decrementAndGet();
    }

    public void onError(Throwable throwable) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onError", throwable);

        if (throwable instanceof ServerShutdownStreamException) {
            session.close(StatusCode.SHUTDOWN, throwable.getMessage());
        } else {
            // Send exception
            // Client should receive exception and close session
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream out = new ExceptionObjectOutputStream(bout);
                out.writeObject(throwable);
                out.close();
                String base64Throwable = Base64.getEncoder().encodeToString(bout.toByteArray());
                session.getRemote().sendString(base64Throwable);
                session.getRemote().flush();
            } catch (IOException e) {
                logger.error(marker, "Could not send exception", e);
            }
        }
    }

    public void onComplete() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onComplete");

        isComplete.set(true);
        if (!isDraining.get()) {
            CompletableFuture.runAsync(this::drainJob);
        }
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);

        Optional.ofNullable(session.getUpgradeRequest().getParameterMap().get("configuration")).map(l -> l.get(0)).ifPresent(this::applyConfiguration);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketText {}", message);

        if (publisherConfiguration == null) {
            applyConfiguration(message);
            return;
        }

        long requestAmount = Long.parseLong(message);

        if (subscription != null) {
            if (requestAmount == Long.MIN_VALUE) {
                logger.info(marker, "Received cancel on websocket " + streamName);
                session.close(StatusCode.NORMAL, "cancelled");
            } else {
                if (logger.isTraceEnabled())
                    logger.trace(marker, "Received request:" + requestAmount);

                if (requestAmount > 0) {
                    requestsHistogram.record(requestAmount, attributes);
                    activeSubscription.requested().addAndGet(requestAmount);
                    subscription.request(requestAmount);
                }
            }
        } else {
            outstandingRequestAmount += requestAmount;
        }
    }

    private void applyConfiguration(String configuration) {
        try {
            ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(configuration);
            publisherConfiguration = new Configuration.Builder(configurationJson)
                    .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                    .build();
        } catch (JsonProcessingException e) {
            logger.error("Could not parse publisher configuration", e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
        }

        try {
            Publisher<Object> publisher = publisherFactory.apply(publisherConfiguration);
            publisher.subscribe(this);
            activeSubscription = new ActiveSubscriptions.ActiveSubscription(streamName, new AtomicLong(), new AtomicLong(), publisherConfiguration);
            activeSubscriptions.addSubscription(activeSubscription);

            logger.debug(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());
        } catch (Throwable e) {
            onError(e);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketBinary");

        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketError", cause);

        if ((cause instanceof ClosedChannelException ||
                cause instanceof WebSocketTimeoutException ||
                cause instanceof EofException)
                && subscription != null) {
            // Ignore
        } else {
            logger.error(marker, "Publisher websocket error", cause);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
            activeSubscriptions.removeSubscription(activeSubscription);
        }
    }

    private void drainJob() {
        drainLock.lock();
        isDraining.set(true);
        try {
            logger.trace(marker, "Start drain");
            int count;
            int itemsSent = 0;
            List<Object> items = new ArrayList<>(sendQueue.size());
            while ((count = sendQueue.drainTo(items)) > 0) {
                for (Object item : items) {
                    send(item);

                    itemsSent++;
                    if (itemsSent == 1024) {
                        if (logger.isTraceEnabled())
                            logger.trace(marker, "flush {}", itemsSent);
                        session.getRemote().flush();
                        flushHistogram.record(itemsSent, attributes);
                        itemsSent = 0;
                    }
                }
                activeSubscription.received().addAndGet(count);
                items.clear();
            }

            if (itemsSent > 0) {
                if (logger.isTraceEnabled())
                    logger.trace(marker, "flush {}", itemsSent);
                session.getRemote().flush();
                flushHistogram.record(itemsSent, attributes);
            }

        } catch (Throwable t) {
            logger.error(marker, "Could not send event", t);
            if (session.isOpen()) {
                session.close(StatusCode.SERVER_ERROR, t.getMessage());
            }
        } finally {
            logger.trace(marker, "Stop drain");

            if (isComplete.get() && session.isOpen()) {
                session.close(StatusCode.NORMAL, "complete");
            }
            isDraining.set(false);
            drainLock.unlock();
        }
    }

    protected void send(Object item) throws IOException {
//        if (logger.isTraceEnabled())
//            logger.trace(marker, "send {}", item.getClass().toString());

        // Write event data
        writeItem(messageWriter, item, outputStream);
        ByteBuffer eventBuffer = outputStream.takeByteBuffer();
        sentBytes.record(eventBuffer.limit(), attributes);
        session.getRemote().sendBytes(eventBuffer);
        pool.release(eventBuffer);
    }

    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }
}
