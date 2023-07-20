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
package com.exoreaction.xorcery.reactivestreams.client;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientBadPayloadStreamException;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class SubscribeReactiveStream
        implements WebSocketPartialListener,
        WebSocketConnectionListener,
        Subscription {

    private final URI serverUri;
    private final String streamName;

    private ClientConfiguration subscriberConfiguration;
    private long retryDelay;

    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;
    protected final Subscriber<Object> subscriber;
    protected final MessageReader<Object> eventReader;
    private final Supplier<Configuration> publisherConfiguration;
    protected final ByteBufferPool byteBufferPool;
    protected final Logger logger;
    protected final CompletableFuture<Void> result;

    protected final Marker marker;
    protected final ByteBufferAccumulator byteBufferAccumulator;
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong outstandingRequests = new AtomicLong();
    private String exceptionPayload = "";

    private Iterator<URI> uriIterator;
    protected Session session;

    protected final Meter received;
    protected final Meter receivedBytes;
    protected final Histogram requestsHistogram;

    protected boolean isComplete; // true if onComplete or onError has been called
    protected boolean isRetrying; // true if we're in the process of retrying/reconnecting
    protected boolean isCancelled; // true if cancel() has been called or the result has been completed

    public SubscribeReactiveStream(URI serverUri,
                                   String streamName,
                                   ClientConfiguration subscriberConfiguration,
                                   DnsLookup dnsLookup,
                                   WebSocketClient webSocketClient,
                                   Subscriber<Object> subscriber,
                                   MessageReader<Object> eventReader,
                                   Supplier<Configuration> publisherConfiguration,
                                   ByteBufferPool pool,
                                   MetricRegistry metricRegistry,
                                   Logger logger,
                                   CompletableFuture<Void> result) {
        this.serverUri = serverUri;
        this.streamName = streamName;
        this.subscriberConfiguration = subscriberConfiguration;
        this.retryDelay = Duration.parse("PT" + subscriberConfiguration.getRetryDelay()).toMillis();
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.subscriber = subscriber;
        this.eventReader = eventReader;
        this.publisherConfiguration = publisherConfiguration;
        this.byteBufferAccumulator = new ByteBufferAccumulator(pool, false);
        this.byteBufferPool = pool;
        this.logger = logger;

        this.received = metricRegistry.meter("subscribe." + streamName + ".received");
        this.receivedBytes = metricRegistry.meter("subscribe." + streamName + ".received.bytes");
        this.requestsHistogram = metricRegistry.histogram("subscribe." + streamName + ".requests");

        this.result = result;

        this.marker = MarkerManager.getMarker(this.serverUri.getAuthority() + "/" + streamName);

/*
        this.result.whenComplete((r, t) ->
        {
            // Check if session is open
            if (session != null) {
                synchronized (session) {
                    isComplete = true;
                    if (session.isOpen()) {

                        WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
                        session.close(StatusCode.NORMAL, "cancel", callback);
                        callback.future().join();

                        if (!isComplete) {
                            isComplete = true;
                            subscriber.onError(t);
                        }
                    }
                }
            }
        });
*/

        subscriber.onSubscribe(this);

        start();
    }

    // Connection process
    public synchronized void start() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "start");
        }

        if (result.isDone()) {
            return;
        }

        if (!webSocketClient.isStarted()) {
            retry(null);
        }

        if (serverUri.getScheme().equals("srv"))
        {
            logger.debug(marker, "Resolving " + serverUri);
            dnsLookup.resolve(serverUri).thenApply(list ->
            {
                this.uriIterator = list.iterator();
                return uriIterator;
            }).thenAccept(this::connect).exceptionally(this::connectException);
        } else
        {
            this.uriIterator = List.of(serverUri).iterator();
            connect(uriIterator);
        }
    }

    private synchronized void connect(Iterator<URI> publisherURIs) {

        if (publisherURIs.hasNext()) {
            URI publisherWebsocketUri = publisherURIs.next();

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", publisherWebsocketUri);
            }

            URI effectivePublisherWebsocketUri = URI.create(publisherWebsocketUri.getScheme() + "://" + publisherWebsocketUri.getAuthority() + "/streams/publishers/" + streamName);
            logger.debug(marker, "Trying " + effectivePublisherWebsocketUri);
            try {
                webSocketClient.connect(this, effectivePublisherWebsocketUri)
                        .thenAccept(this::connected)
                        .exceptionally(this::connectException);
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + effectivePublisherWebsocketUri.toASCIIString(), e);
                retry(e);
            }
        } else {
            retry(null);
        }
    }

    private void connected(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "connected");
        }
        this.session = session;
        this.isRetrying = false;
    }

    private Void connectException(Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "exceptionally", throwable);
        }
        retry(throwable);
        return null;
    }

    public synchronized void retry(Throwable cause) {
        if (subscriberConfiguration.isRetryEnabled()) {

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "retry");
            }

            if (!isRetrying) {
                logger.debug(marker, "Retrying");
                isRetrying = true;
            }

            requests.set(outstandingRequests.get());

            if (uriIterator.hasNext()) {
                connect(uriIterator);
            } else {
                if (cause instanceof ServerTimeoutStreamException) {
                    start();
                } else {
                    CompletableFuture.delayedExecutor(retryDelay, TimeUnit.MILLISECONDS).execute(this::start);
                    // Exponential backoff, gets reset on successful connect, max 60s delay
                    retryDelay = Math.min(retryDelay * 2, 60000);
                }
            }
        } else {
            if (cause == null) {
                if (!isComplete) {
                    isComplete = true;
                    subscriber.onComplete();
                }
                result.cancel(true);
            } else {
                if (!isComplete) {
                    isComplete = true;
                    subscriber.onError(cause);
                }
                result.completeExceptionally(cause);
            }
        }
    }

    // Subscription
    @Override
    public synchronized void request(long n) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "request {}", n);
        }

        requests.addAndGet(n);
        outstandingRequests.addAndGet(n);

        if (session != null) {
            if (isCancelled || isComplete) {
                return;
            }
            if (session.isOpen())
                CompletableFuture.runAsync(this::sendRequests);
        }
    }

    @Override
    public synchronized void cancel() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "cancel");
        }

        if (session != null) {
            synchronized (session) {
                isCancelled = true;
                requests.set(Long.MIN_VALUE);
                if (session.isOpen())
                    CompletableFuture.runAsync(this::sendRequests);
            }
        }
    }

    // Websocket
    @Override
    public synchronized void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }
        this.session = session;
        this.retryDelay = Duration.parse("PT" + subscriberConfiguration.getRetryDelay()).toMillis();

        // First send parameters, if available
        String parameterString = publisherConfiguration.get().json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAcceptAsync(Void ->
                {
                    logger.debug(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
                    CompletableFuture.runAsync(this::sendRequests);
                }).exceptionally(t ->
                {
                    session.close(StatusCode.SERVER_ERROR, t.getMessage());
                    logger.error(marker, "Parameter handshake failed", t);
                    return null;
                })
        ));
    }

    @Override
    public synchronized void onWebSocketPartialText(String payload, boolean fin) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketPartialText {} {}", payload, fin);
        }

        exceptionPayload += payload;
        if (fin) {
            Throwable throwable;
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(Base64.getDecoder().decode(exceptionPayload));
                ObjectInputStream oin = new ObjectInputStream(bin);
                throwable = (Throwable) oin.readObject();
            } catch (Throwable e) {
                throwable = e;
            }

            if (!isComplete) {
                isComplete = true;
                subscriber.onError(throwable);
            }

            // Ack to publisher
            WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
            session.close(StatusCode.NORMAL, "onError", callback);
            Throwable finalThrowable = throwable;
            callback.future().handle((v, t) -> result.completeExceptionally(finalThrowable));
        }
    }

    @Override
    public synchronized void onWebSocketPartialBinary(ByteBuffer payload, boolean fin) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketPartialBinary {} {}", Charset.defaultCharset().decode(payload.asReadOnlyBuffer()).toString(), fin);
        }

        byteBufferAccumulator.copyBuffer(payload);
        if (fin) {
            onWebSocketBinary(byteBufferAccumulator.takeByteBuffer());
            byteBufferAccumulator.close();
        }
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            received.mark();
            receivedBytes.mark(byteBuffer.position());
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
            subscriber.onNext(event);
            outstandingRequests.decrementAndGet();
        } catch (IOException e) {
            logger.error("Could not receive value", e);

            if (!isComplete) {
                isComplete = true;
                subscriber.onError(e);
            }

            // These kinds of errors are not retryable, we're done
            WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
            session.close(StatusCode.NORMAL, "onError", callback);
            callback.future().handle((v, t) -> result.completeExceptionally(e));
        }
    }

    @Override
    public synchronized void onWebSocketError(Throwable cause) {
        cause = unwrap(cause);

        if (!(cause instanceof ClosedChannelException)) {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketError", cause);
            }
        }
    }

    @Override
    public synchronized void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        if (statusCode == StatusCode.NORMAL) {
            try {
                if (!isComplete) {
                    isComplete = true;
                    subscriber.onComplete();
                }
            } catch (Exception e) {
                logger.warn(marker, "Could not close subscription", e);
            }
            logger.debug(marker, "Complete subscription:{} {}", statusCode, reason);
            result.complete(null); // Now considered done
        } else if (statusCode == StatusCode.BAD_PAYLOAD) {
            try {
                if (!result.isCompletedExceptionally() && !isComplete) {
                    isComplete = true;
                    subscriber.onError(new ClientBadPayloadStreamException(reason));
                }
            } catch (Exception e) {
                logger.warn(marker, "Could not close subscription", e);
            }
            logger.warn(marker, "Bad payload:{} {}", statusCode, reason);
            result.completeExceptionally(new ClientBadPayloadStreamException(reason)); // Now considered done
        } else if (statusCode == StatusCode.SHUTDOWN) {
            if (reason.equals("Connection Idle Timeout")) {
                logger.debug(marker, "Server timeout:{} {}", statusCode, reason);
                retry(new ServerTimeoutStreamException("Server closed due to timeout"));
            } else {
                logger.warn(marker, "Server is shutting down:{} {}", statusCode, reason);
                retry(new ServerShutdownStreamException("Server is shutting down"));
            }
        } else {
            logger.warn(marker, "Websocket failed, retrying:{} {}", statusCode, reason);
            retry(new ServerStreamException("Websocket failed:" + reason));
        }
    }

    // Send requests
    public synchronized void sendRequests() {
        try {
            if (!session.isOpen()) {
                logger.debug(marker, "Session closed, cannot send requests");
                return;
            }

            final long rn = requests.getAndSet(0);
            if (rn == 0) {
                if (isCancelled) {
                    logger.debug(marker, "Subscription cancelled");
                }
                return;
            }

            logger.trace(marker, "Sending request: {}", rn);
            requestsHistogram.update(rn);
            session.getRemote().sendString(Long.toString(rn), new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    logger.error(marker, "Could not send requests {}", rn, x);
                }

                @Override
                public void writeSuccess() {
                    logger.trace(marker, "Successfully sent request {}", rn);
                }
            });

            try {
                logger.trace(marker, "Flushing remote session...");
                session.getRemote().flush();
                logger.trace(marker, "Remote session flushed.");
            } catch (IOException e) {
                logger.error(marker, "While flushing remote session", e);
            }
        } catch (Throwable t) {
            logger.error(marker, "Error sending requests", t);
        }
    }
}
