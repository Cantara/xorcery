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
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.common.ActiveSubscriptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.*;
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
    private final BlockingArrayQueue<Object> sendQueue = new BlockingArrayQueue<>(4096);
    private AtomicBoolean isDraining = new AtomicBoolean();
    private Lock drainLock = new ReentrantLock();
    private final ByteBufferOutputStream2 outputStream;

    protected final MessageWriter<Object> messageWriter;

    private final ObjectMapper objectMapper;
    protected final ByteBufferPool pool;
    protected final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private long outstandingRequestAmount;
    private ActiveSubscriptions.ActiveSubscription activeSubscription;

    private final Meter sent;
    private final Meter sentBytes;
    private final Histogram requestsHistogram;
    private final Histogram flushHistogram;

    public PublisherSubscriptionReactiveStream(String streamName,
                                               Function<Configuration, Publisher<Object>> publisherFactory,
                                               MessageWriter<Object> messageWriter,
                                               ObjectMapper objectMapper,
                                               ByteBufferPool pool,
                                               Logger logger,
                                               ActiveSubscriptions activeSubscriptions,
                                               MetricRegistry metricRegistry) {
        this.streamName = streamName;
        this.publisherFactory = publisherFactory;
        this.messageWriter = messageWriter;
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(streamName);

        outputStream = new ByteBufferOutputStream2(pool, true);
        this.logger = logger;
        this.activeSubscriptions = activeSubscriptions;

        this.sent = metricRegistry.meter("publisher." + streamName + ".sent");
        this.sentBytes = metricRegistry.meter("publisher." + streamName + ".sent.bytes");
        this.requestsHistogram = metricRegistry.histogram("publisher." + streamName + ".requests");
        this.flushHistogram = metricRegistry.histogram("publisher." + streamName + ".flush.size");
    }

    // Subscriber
    @Override
    public synchronized void onSubscribe(Subscription subscription) {

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

        sendQueue.offer(item);
        if (!isDraining.get()) {
            drainQueue();
        }
        activeSubscription.requested().decrementAndGet();
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
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketText {}", message);

        synchronized (this) {
            if (publisherConfiguration == null) {
                // Read JSON parameters
                try {
                    ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(message);
                    publisherConfiguration = new Configuration.Builder(configurationJson)
                            .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                            .build();
                    Publisher<Object> publisher = publisherFactory.apply(publisherConfiguration);
                    publisher.subscribe(this);

                    activeSubscription = new ActiveSubscriptions.ActiveSubscription(streamName, new AtomicLong(), new AtomicLong(), publisherConfiguration);
                    activeSubscriptions.addSubscription(activeSubscription);
                    return;
                } catch (JsonProcessingException e) {
                    session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
                }
            }
        }

        long requestAmount = Long.parseLong(message);

        if (subscription != null) {
            if (requestAmount == Long.MIN_VALUE) {
                logger.info(marker, "Received cancel on websocket " + streamName);
                session.close(StatusCode.NORMAL, "cancelled");
            } else {
                if (logger.isDebugEnabled())
                    logger.debug(marker, "Received request:" + requestAmount);

                if (requestAmount > 0) {
                    activeSubscription.requested().addAndGet(requestAmount);
                    subscription.request(requestAmount);
                    requestsHistogram.update(requestAmount);
                }
            }
        } else {
            outstandingRequestAmount += requestAmount;
        }
    }

    @Override
    public synchronized void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketBinary");

        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public synchronized void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
            activeSubscriptions.removeSubscription(activeSubscription);
        }
    }

    @Override
    public synchronized void onWebSocketError(Throwable cause) {
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

    private void drainQueue() {
        isDraining.set(true);
        CompletableFuture.runAsync(this::drainJob);
    }

    private void drainJob() {
        drainLock.lock();
        try {
            logger.trace(marker, "Start drain");
            List<Object> items = new ArrayList<>(4096);
            int count;
            int itemsSent = 0;
            while ((count = sendQueue.drainTo(items)) > 0) {
                for (Object item : items) {
                    send(item);
                }
                itemsSent += count;

                if (itemsSent > 1024) {
                    if (logger.isTraceEnabled())
                        logger.trace(marker, "flush {}", itemsSent);
                    session.getRemote().flush();
                    flushHistogram.update(itemsSent);
                    itemsSent = 0;
                }
                activeSubscription.received().addAndGet(count);
                items.clear();
            }

            if (itemsSent > 0) {
                if (logger.isTraceEnabled())
                    logger.trace(marker, "flush {}", itemsSent);
                session.getRemote().flush();
                flushHistogram.update(itemsSent);
            }

            isDraining.set(false);
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
            drainLock.unlock();
        }
    }

    protected void send(Object item) throws IOException {
//        if (logger.isTraceEnabled())
//            logger.trace(marker, "send {}", item.getClass().toString());

        // Write event data
        writeItem(messageWriter, item, outputStream);
        ByteBuffer eventBuffer = outputStream.takeByteBuffer();
        sent.mark();
        sentBytes.mark(eventBuffer.limit());
        session.getRemote().sendBytes(eventBuffer);
        pool.release(eventBuffer);
    }

    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }

    public synchronized void onError(Throwable throwable) {
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

        if (!isDraining.get()) {
            if (session.isOpen()) {
                session.close(StatusCode.NORMAL, "complete");
            }
        } else {
            // Wait for requests to drain the remaining items
            isComplete.set(true);
        }
    }
}
