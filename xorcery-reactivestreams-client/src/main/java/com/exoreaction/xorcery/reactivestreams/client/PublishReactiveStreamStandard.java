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
import com.exoreaction.xorcery.net.URIs;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author rickardoberg
 */
public class PublishReactiveStreamStandard
        implements WebSocketListener,
        WriteCallback,
        Flow.Subscriber<Object> {

    private final static Logger logger = LogManager.getLogger(PublishReactiveStreamStandard.class);

    private final WebSocketClient webSocketClient;
    private final DnsLookup dnsLookup;
    protected final ByteBufferPool pool;
    protected final MessageWriter<Object> eventWriter;

    private final ClientConfiguration publisherConfiguration;
    private final Supplier<Configuration> subscriberConfiguration;
    protected final CompletableFuture<Void> result;
    protected final Marker marker;

    protected final Meter sent;
    protected final Meter sentBytes;
    protected final Histogram sentBatchSize;
    protected final Histogram requestsHistogram;
    private final long maxBatchSize = 4096;
    protected long batchSize;

    private boolean redundancyNotificationIssued = false;

    private final URI serverUri;
    private final String streamName;
    private final Flow.Publisher<Object> publisher;

    // All the state which requires synchronized access
    private Iterator<URI> uriIterator;
    protected Session session;
    protected Flow.Subscription subscription;
    private long retryDelay;
    private final Deque<Object> queue = new ArrayDeque<>();
    protected final ByteBufferOutputStream2 outputStream;
    private long requested = 0;
    private ByteBuffer eventBuffer;
    private Object item;
    private boolean isSending = false;
    protected boolean isComplete = false;

    public PublishReactiveStreamStandard(URI serverUri,
                                         String streamName,
                                         ClientConfiguration publisherConfiguration,
                                         DnsLookup dnsLookup,
                                         WebSocketClient webSocketClient,
                                         Flow.Publisher<Object> publisher,
                                         MessageWriter<Object> eventWriter,
                                         Supplier<Configuration> subscriberConfiguration,
                                         ByteBufferPool pool,
                                         MetricRegistry metricRegistry,
                                         CompletableFuture<Void> result) {
        this.serverUri = serverUri;
        this.streamName = streamName;
        this.publisherConfiguration = publisherConfiguration;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.publisher = publisher;
        this.eventWriter = eventWriter;
        this.subscriberConfiguration = subscriberConfiguration;
        this.pool = pool;
        this.result = result;
        this.retryDelay = Duration.parse("PT" + publisherConfiguration.getRetryDelay()).toMillis();
        this.marker = MarkerManager.getMarker(serverUri.getAuthority() + "/" + streamName);
        this.sent = metricRegistry.meter("publish." + streamName + ".sent");
        this.sentBytes = metricRegistry.meter("publish." + streamName + ".sent.bytes");
        this.sentBatchSize = metricRegistry.histogram("publish." + streamName + ".sent.batchsize");
        this.requestsHistogram = metricRegistry.histogram("publish." + streamName + ".requests");
        this.outputStream = new ByteBufferOutputStream2(pool, true);

        start();
    }

    // Subscriber
    @Override
    public synchronized void onSubscribe(Flow.Subscription subscription) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onSubscribe");
        }

        this.subscription = subscription;
    }

    @Override
    public synchronized void onNext(Object item) {
        if (logger.isTraceEnabled()) {
//            logger.trace(marker, "onNext {}", item.toString());
        }

        queue.offer(item);

        if (!isSending)
            send();
    }

    @Override
    public synchronized void onError(Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onError", throwable);
        }

        if (!result.isDone()) {
            if (throwable instanceof ServerStreamException) {
                // This should basically never happen
                session.close(StatusCode.SERVER_ERROR, throwable.getMessage());
            } else {
                session.close(StatusCode.NORMAL, throwable.getMessage());
            }
            result.completeExceptionally(throwable);
        }
    }

    public synchronized void onComplete() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onComplete");
        }
        isComplete = true;
        if (!isSending)
            send();
        logger.debug(marker, "Waiting for outstanding events to be sent to {}", session.getRemote().getRemoteAddress());
    }

    // Connection process
    public void start() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "start");
        }

        if (result.isDone()) {
            return;
        }

        if (!webSocketClient.isStarted()) {
            retry(null);
        }

        logger.info(marker, "Resolving " + serverUri);
        dnsLookup.resolve(serverUri).thenApply(list ->
        {
            this.uriIterator = list.iterator();
            return uriIterator;
        }).thenAccept(this::connect).whenComplete(this::exceptionally);
    }

    private void connect(Iterator<URI> subscriberURIs) {
        if (subscriberURIs.hasNext()) {
            URI subscriberWebsocketUri = subscriberURIs.next();

            if ("https".equals(subscriberWebsocketUri.getScheme()))
                subscriberWebsocketUri = URIs.withScheme(subscriberWebsocketUri, "wss");
            else if ("http".equals(subscriberWebsocketUri.getScheme()))
                subscriberWebsocketUri = URIs.withScheme(subscriberWebsocketUri, "ws");

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", subscriberWebsocketUri);
            }

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", subscriberWebsocketUri);
            }

            URI effectiveSubscriberWebsocketUri = URI.create(subscriberWebsocketUri.getScheme() + "://" + subscriberWebsocketUri.getAuthority() + "/streams/subscribers/" + streamName);
            logger.debug(marker, "Trying " + effectiveSubscriberWebsocketUri);
            try {
                webSocketClient.connect(this, effectiveSubscriberWebsocketUri)
                        .whenComplete(this::exceptionally);
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + effectiveSubscriberWebsocketUri.toASCIIString(), e);
                retry(e);
            }
        } else {
            retry(null);
        }
    }

    private <T> void exceptionally(T value, Throwable throwable) {
        if (throwable != null) {

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "exceptionally", throwable);
            }

            if (subscription != null) {
                subscription.cancel();
                subscription = null;
            }
            if (session != null) {
                session.close(StatusCode.NORMAL, throwable.getMessage());
                session = null;
            }

            if (throwable instanceof SSLHandshakeException) {
                // Give up
                result.completeExceptionally(throwable);
            } else {
                // TODO Handle more exceptions
                retry(throwable);
            }
        }
    }

    public void retry(Throwable cause) {

        if (logger.isTraceEnabled()) {
            logger.trace(marker, "retry");
        }

        if (isComplete || result.isDone()) {
            return;
        }

        if (publisherConfiguration.isRetryEnabled()) {

            // Reset state
            requested = 0;

            if (uriIterator.hasNext()) {
                connect(uriIterator);
            } else {
                if (cause instanceof ServerShutdownStreamException) {
                    retryDelay = 0;
                }
                CompletableFuture.delayedExecutor(retryDelay, TimeUnit.MILLISECONDS).execute(this::start);
                // Exponential backoff, gets reset on successful connect, max 60s delay
                retryDelay = Math.min(retryDelay * 2, 60000);
            }
        } else {
            if (cause == null)
                result.cancel(true);
            else
                result.completeExceptionally(cause);
        }
    }

    // WebSocket
    @Override
    public synchronized void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }

        this.session = session;
        this.requested = 0;
        this.retryDelay = Duration.parse("PT" + publisherConfiguration.getRetryDelay()).toMillis();

        // First send parameters, if available
        String parameterString = subscriberConfiguration.get().json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture(new CompletableFuture<>()
                .thenAccept(Void ->
                {
                    logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
                }).exceptionally(t ->
                {
                    session.close(StatusCode.SERVER_ERROR, t.getMessage());
                    logger.error(marker, "Parameter handshake failed", t);
                    return null;
                })));
    }

    @Override
    public synchronized void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        long requestAmount = Long.parseLong(message);

        if (requestAmount == Long.MIN_VALUE) {
            logger.debug(marker, "Received cancel on websocket");
            session.close();
            session = null;

            // Try another subscriber
            retry(null);
        } else {
/*
            if (logger.isDebugEnabled() && !logger.isTraceEnabled())
                logger.debug(marker, "Received request:" + requestAmount);
*/

            if (subscription == null) {
                publisher.subscribe(this);
            }

            requestsHistogram.update(requestAmount);
            requested += requestAmount;
            subscription.request(requestAmount);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public synchronized void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        session = null;
        // TODO This has to be reviewed to see what codes should cause retry and what should cause cancellation of the process
        if (statusCode == StatusCode.NORMAL) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            result.complete(null);
        } else if (statusCode == StatusCode.SHUTDOWN || statusCode == StatusCode.NO_CLOSE) {
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
            retry(new ServerShutdownStreamException(reason));
        }
    }

    @Override
    public synchronized void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
            if (!result.isDone()) {
                retry(cause);
            }
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketError", cause);
            }

            logger.error(marker, "Publisher websocket error", cause);
            if (subscription != null)
                subscription.cancel();
            result.completeExceptionally(cause); // Now considered done
        }
    }

    protected void send() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "send {}", isSending);
        }

        if (requested > 0 && session != null) {
            item = queue.poll();
            if (item == null) {
                isSending = false;
                checkDone();

                return;
            }

            isSending = true;

            try {
                // Write event data
                writeEvent(item);

                // Send it
                eventBuffer = outputStream.takeByteBuffer();
                session.getRemote().sendBytes(eventBuffer, this);

            } catch (Throwable t) {
                isSending = false;
                logger.error(marker, "Could not send event", t);
                exceptionally(null, t);
            }
        } else {
            isSending = false;
            checkDone();
        }
    }

    protected void checkDone()
    {
        if (isComplete) {
            if (session != null) {
                logger.debug(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
                session.close(StatusCode.NORMAL, "complete");
            }
            result.complete(null);
        }
    }

    protected void writeEvent(Object item) throws IOException {
        eventWriter.writeTo(item, outputStream);
    }

    @Override
    public synchronized void writeFailed(Throwable t) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "writeFailed", t);
        }

        queue.push(item);
        pool.release(eventBuffer);
        onWebSocketError(t);
    }

    @Override
    public void writeSuccess() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "writeSuccess");
        }

//        synchronized (session)
        {
            synchronized (this) {
                sentBytes.mark(eventBuffer.position());
                pool.release(eventBuffer);

                requested--;
                batchSize++;
                if (requested == 0 || queue.isEmpty() || batchSize == maxBatchSize) {
//                    logger.debug(marker, "doFlush {} {} {}", requested, queue.size(), batchSize);
                    CompletableFuture.runAsync(this::flush);
                } else {
                    send();
                }
            }
        }
    }

    protected void flush() {
        if (logger.isDebugEnabled()) {
//            logger.debug(marker, "flush {}", batchSize);
        }
        try {
            session.getRemote().flush();

            synchronized (this) {
                sent.mark(batchSize);
                sentBatchSize.update(batchSize);
                batchSize = 0;
                send();
            }
        } catch (IOException e) {
            // TODO Add retry for the items in this batch here
            logger.error(marker, "Could not send event", e);
            exceptionally(null, e);
        }
    }
}
