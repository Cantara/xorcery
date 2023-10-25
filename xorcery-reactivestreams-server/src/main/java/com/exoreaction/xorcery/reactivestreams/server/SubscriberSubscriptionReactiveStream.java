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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.common.ActiveSubscriptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class SubscriberSubscriptionReactiveStream
        extends ServerReactiveStream
        implements WebSocketPartialListener,
        WebSocketConnectionListener,
        Subscription {
    private final String streamName;
    private final Function<Configuration, Subscriber<Object>> subscriberFactory;
    private String configurationMessage = "";
    private Configuration subscriberConfiguration;
    protected Subscriber<Object> subscriber;

    protected final MessageReader<Object> eventReader;

    protected final ByteBufferAccumulator byteBufferAccumulator;
    protected final ByteBufferPool byteBufferPool;

    protected final ObjectMapper objectMapper;
    protected final Marker marker;
    protected final Logger logger;

    protected Session session;

    protected final Meter received;
    protected final Meter receivedBytes;
    protected final Histogram requestsHistogram;

    // Subscription
    private final ActiveSubscriptions activeSubscriptions;
    private ActiveSubscriptions.ActiveSubscription activeSubscription;
    private final AtomicLong requests = new AtomicLong();
    private boolean isCancelled;
    private boolean isSendingRequests;

    public SubscriberSubscriptionReactiveStream(String streamName,
                                                Function<Configuration, Subscriber<Object>> subscriberFactory,
                                                MessageReader<Object> eventReader,
                                                ObjectMapper objectMapper,
                                                ByteBufferPool byteBufferPool,
                                                MetricRegistry metricRegistry,
                                                Logger logger,
                                                ActiveSubscriptions activeSubscriptions) {
        this.streamName = streamName;
        this.subscriberFactory = subscriberFactory;
        this.eventReader = eventReader;
        this.objectMapper = objectMapper;
        this.byteBufferPool = byteBufferPool;
        this.byteBufferAccumulator = new ByteBufferAccumulator(byteBufferPool, false);
        this.marker = MarkerManager.getMarker(streamName);
        this.logger = logger;
        this.activeSubscriptions = activeSubscriptions;

        this.received = metricRegistry.meter("subscriber." + streamName + ".received");
        this.receivedBytes = metricRegistry.meter("subscriber." + streamName + ".received.bytes");
        this.requestsHistogram = metricRegistry.histogram("subscriber." + streamName + ".requests");
    }

    // Subscription
    @Override
    public synchronized void request(long n) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "request {}", n);

        if (isCancelled) {
            return;
        }
        requests.addAndGet(n);
        if (session != null)
            CompletableFuture.runAsync(this::sendRequests);
    }

    @Override
    public synchronized void cancel() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "cancel");

        isCancelled = true;
        requests.set(Long.MIN_VALUE);
        if (session != null)
            CompletableFuture.runAsync(this::sendRequests);
    }

    // WebSocket
    @Override
    public synchronized void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public synchronized void onWebSocketPartialText(String message, boolean fin) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketPartialText {} {}", message, fin);

        if (subscriberConfiguration == null) {
            configurationMessage += message;

            if (fin) {
                // Read JSON parameters
                try {
                    ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(configurationMessage);
                    subscriberConfiguration = new Configuration.Builder(configurationJson)
                            .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                            .build();
                } catch (JsonProcessingException e) {
                    logger.error("Could not parse subscriber configuration", e);
                    session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
                }

                try {
                    subscriber = subscriberFactory.apply(subscriberConfiguration);
                    subscriber.onSubscribe(this);

                    activeSubscription = new ActiveSubscriptions.ActiveSubscription(streamName, new AtomicLong(), new AtomicLong(), subscriberConfiguration);
                    activeSubscriptions.addSubscription(activeSubscription);

                    logger.debug(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());
                } catch (Throwable e) {
                    // TODO Send exception here
                }
            }
        }
    }

    @Override
    public synchronized void onWebSocketPartialBinary(ByteBuffer payload, boolean fin) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketPartialBinary {} {}", fin, payload.limit());

        byteBufferAccumulator.copyBuffer(payload);
        if (fin) {
            onWebSocketBinary(byteBufferAccumulator.takeByteBuffer());
            byteBufferAccumulator.close();
        }
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()).toString());
            }
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            received.mark();
            receivedBytes.mark(byteBuffer.position());
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
            subscriber.onNext(event);
            activeSubscription.requested().decrementAndGet();
            activeSubscription.received().incrementAndGet();
        } catch (Throwable e) {
            logger.error("Could not receive value", e);
            subscriber.onError(e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
        }
    }

    @Override
    public synchronized void onWebSocketError(Throwable cause) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketError", cause);

        switch (cause.getClass().getName()) {
            case "java.nio.channels.AsynchronousCloseException",
                    "org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException",
                    "org.eclipse.jetty.io.EofException" -> {
                // Do nothing
            }
            default -> {
                logger.warn(marker, "Subscriber websocket error", cause);
                if (subscriber != null) {
                    subscriber.onError(cause);
                }
            }
        }
    }

    @Override
    public synchronized void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        try {
            if (statusCode == StatusCode.NORMAL) {
                if (subscriber != null) {
                    subscriber.onComplete();
                }
            } else if (statusCode == StatusCode.SHUTDOWN) {
                if (subscriber != null) {
                    Throwable throwable;
                    if (reason.equals("Connection Idle Timeout")) {
                        throwable = new ServerTimeoutStreamException(reason);
                    } else {
                        throwable = new ClientShutdownStreamException(reason);
                    }
                    subscriber.onError(throwable);
                }
            }
            activeSubscriptions.removeSubscription(activeSubscription);
        } catch (Exception e) {
            logger.warn(marker, "Could not close subscription sink", e);
        }
    }

    // Send requests
    protected void sendRequests() {
        try {

            synchronized (this) {
                if (isSendingRequests)
                    return;

                if (!session.isOpen()) {
                    logger.debug(marker, "Session closed, cannot send requests");
                    return;
                }

                isSendingRequests = true;
            }

            long rn;
            while ((rn = requests.getAndSet(0)) != 0) {

                if (rn != Long.MIN_VALUE)
                    requestsHistogram.update(rn);
                final long finalRn = rn;
                session.getRemote().sendString(Long.toString(rn), new WriteCallback() {
                    @Override
                    public void writeFailed(Throwable x) {
                        logger.error(marker, "Could not send requests {}", finalRn, x);
                    }

                    @Override
                    public void writeSuccess() {
                        activeSubscription.requested().addAndGet(finalRn);
                    }
                });

                try {
                    session.getRemote().flush();
                    logger.trace(marker, "Flushed remote session");
                } catch (IOException e) {
                    logger.error(marker, "While flushing remote session", e);
                }
            }
        } catch (Throwable t) {
            logger.error(marker, "Error sending requests", t);
        } finally {
            synchronized (this) {
                isSendingRequests = false;
            }
        }
    }
}
