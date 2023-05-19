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
package com.exoreaction.xorcery.service.reactivestreams.client;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.api.ClientConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.api.ClientStreamException;
import com.exoreaction.xorcery.service.reactivestreams.api.ServerStreamException;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author rickardoberg
 */
public class PublishReactiveStream
        implements WebSocketListener,
        Flow.Subscriber<Object>,
        EventHandler<AtomicReference<Object>> {

    private final static Logger logger = LogManager.getLogger(PublishReactiveStream.class);

    private final static Pattern uriStartsWithSchemeAndAuthorityPattern = Pattern.compile(".+://.+");

    protected Session session;

    private final ClientConfiguration publisherConfiguration;
    private long retryDelay;

    private DnsLookup dnsLookup;
    private WebSocketClient webSocketClient;
    private final Flow.Publisher<Object> publisher;
    protected Flow.Subscription subscription;
    protected final Semaphore semaphore = new Semaphore(0);

    protected final MessageWriter<Object> eventWriter;

    private final Supplier<Configuration> subscriberConfiguration;
    private ScheduledExecutorService timer;
    protected final ByteBufferPool pool;
    protected CompletableFuture<Void> result;
    protected final Marker marker;

    protected final Meter sent;
    protected final Meter sentBytes;
    protected final Histogram sentBatchSize;
    protected final Histogram requestsHistogram;
    protected long batchSize;

    private boolean redundancyNotificationIssued = false;

    protected Disruptor<AtomicReference<Object>> disruptor;
    private final String scheme;
    private final String authority;
    private final String streamName;
    private Iterator<URI> uriIterator;

    public PublishReactiveStream(String defaultScheme,
                                 String authorityOrBaseUri,
                                 String streamName,
                                 ClientConfiguration publisherConfiguration,
                                 DnsLookup dnsLookup,
                                 WebSocketClient webSocketClient,
                                 Flow.Publisher<Object> publisher,
                                 MessageWriter<Object> eventWriter,
                                 Supplier<Configuration> subscriberConfiguration,
                                 ScheduledExecutorService timer,
                                 ByteBufferPool pool,
                                 MetricRegistry metricRegistry,
                                 CompletableFuture<Void> result) {
        this.streamName = streamName;
        this.publisherConfiguration = publisherConfiguration;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.publisher = publisher;
        this.eventWriter = eventWriter;
        this.subscriberConfiguration = subscriberConfiguration;
        this.timer = timer;
        this.pool = pool;
        this.result = result;
        this.retryDelay = Duration.parse("PT" + publisherConfiguration.getRetryDelay()).toMillis();
        this.marker = MarkerManager.getMarker(authorityOrBaseUri + "/" + streamName);
        this.sent = metricRegistry.meter("publish." + streamName + ".sent");
        this.sentBytes = metricRegistry.meter("publish." + streamName + ".sent.bytes");
        this.sentBatchSize = metricRegistry.histogram("publish." + streamName + ".sent.batchsize");
        this.requestsHistogram = metricRegistry.histogram("publish." + streamName + ".requests");

        if (uriStartsWithSchemeAndAuthorityPattern.matcher(authorityOrBaseUri).matches()) {
            URI uri = URI.create(authorityOrBaseUri);
            this.scheme = uri.getScheme();
            if (uri.getPath() != null) {
                this.authority = uri.getAuthority() + uri.getPath();
            } else {
                this.authority = uri.getAuthority();
            }
        } else {
            this.authority = authorityOrBaseUri;
            this.scheme = publisherConfiguration.getScheme().orElse(defaultScheme);
        }

        this.disruptor = new Disruptor<>(AtomicReference::new, publisherConfiguration.getDisruptorSize(), new NamedThreadFactory("PublishWebSocketDisruptor-" + marker.getName() + "-"));
        disruptor.handleEventsWith(this);
        disruptor.start();
        start();
    }

    // Subscriber
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onSubscribe");
        }

        this.subscription = subscription;
    }

    @Override
    public void onNext(Object item) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onNext {}", item.toString());
        }

        disruptor.publishEvent((e, s, event) ->
        {
            e.set(event);
        }, item);
    }

    @Override
    public void onError(Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onError", throwable);
        }

        if (!result.isDone()) {
            disruptor.shutdown();
            if (throwable instanceof ServerStreamException) {
                // This should basically never happen
                session.close(StatusCode.SERVER_ERROR, throwable.getMessage());
            } else {
                session.close(StatusCode.NORMAL, throwable.getMessage());
            }
            result.completeExceptionally(throwable);
        }
    }

    public void onComplete() {
        CompletableFuture.runAsync(() ->
        {
            logger.debug(marker, "Waiting for outstanding events to be sent to {}", session.getRemote().getRemoteAddress());
            disruptor.shutdown();
            logger.debug(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
            session.close(StatusCode.NORMAL, "complete");
            result.complete(null);
        });
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

        URI uri = URI.create(scheme + "://" + authority);
        logger.info(marker, "Resolving " + uri);
        dnsLookup.resolve(uri).thenApply(list ->
        {
            this.uriIterator = list.iterator();
            return uriIterator;
        }).thenAccept(this::connect).whenComplete(this::exceptionally);
    }

    private void connect(Iterator<URI> subscriberURIs) {
        if (subscriberURIs.hasNext()) {
            URI subscriberWebsocketUri = subscriberURIs.next();

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", subscriberWebsocketUri);
            }

            URI schemaAdjustedSubscriberWebsocketUri = subscriberWebsocketUri;
            if (subscriberWebsocketUri.getScheme() != null) {
                if (subscriberWebsocketUri.getScheme().equals("https")) {
                    try {
                        schemaAdjustedSubscriberWebsocketUri = new URI(
                                "wss",
                                subscriberWebsocketUri.getAuthority(),
                                subscriberWebsocketUri.getPath(),
                                subscriberWebsocketUri.getQuery(),
                                subscriberWebsocketUri.getFragment()
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else if (subscriberWebsocketUri.getScheme().equals("http")) {
                    try {
                        schemaAdjustedSubscriberWebsocketUri = new URI(
                                "ws",
                                subscriberWebsocketUri.getAuthority(),
                                subscriberWebsocketUri.getPath(),
                                subscriberWebsocketUri.getQuery(),
                                subscriberWebsocketUri.getFragment()
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            URI effectiveSubscriberWebsocketUri = URI.create(schemaAdjustedSubscriberWebsocketUri.getScheme() + "://" + schemaAdjustedSubscriberWebsocketUri.getAuthority() + "/streams/subscribers/" + streamName);
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

        if (result.isDone()) {
            return;
        }

        if (!uriIterator.hasNext()) {
            if (publisherConfiguration.isRetryEnabled()) {

                timer.schedule(this::start, retryDelay, TimeUnit.MILLISECONDS);
                // Exponential backoff, gets reset on successful connect, max 60s delay
                retryDelay = Math.min(retryDelay*2, 60000);
            } else
            {
                if (cause == null)
                    result.cancel(true);
                else
                    result.completeExceptionally(cause);
            }

        } else
            connect(uriIterator);
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }

        this.session = session;
        this.retryDelay = Duration.parse("PT" + publisherConfiguration.getRetryDelay()).toMillis();

        // First send parameters, if available
        String parameterString = subscriberConfiguration.get().json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAccept(Void ->
                {
                    logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
                }).exceptionally(t ->
                {
                    session.close(StatusCode.SERVER_ERROR, t.getMessage());
                    logger.error(marker, "Parameter handshake failed", t);
                    return null;
                })
        ));
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        long requestAmount = Long.parseLong(message);

        if (requestAmount == Long.MIN_VALUE) {
            logger.debug(marker, "Received cancel on websocket");
            session.close();
            session = null;
            retry(null);
        } else {
            if (logger.isDebugEnabled())
                logger.debug(marker, "Received request:" + requestAmount);

            if (subscription == null) {
                publisher.subscribe(this);
            }

            requestsHistogram.update(requestAmount);
            semaphore.release((int) requestAmount);
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
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        session = null;
        // TODO This has to be reviewed to see what codes should cause retry and what should cause cancellation of the process
        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            result.complete(null);
        } else {
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
            retry(null);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketError", cause);
        }

        if (cause instanceof ClosedChannelException) {
            // Ignore
            if (!result.isDone()) {
                retry(cause);
            }
        } else {
            logger.error(marker, "Publisher websocket error", cause);
            if (subscription != null)
                subscription.cancel();
            result.completeExceptionally(cause); // Now considered done
        }
    }

    // EventHandler
    AtomicReference<Boolean> isShutdown = new AtomicReference<>(false);

    @Override
    public void onShutdown() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onShutdown");
        }

        isShutdown.set(true);
    }

    @Override
    public void onEvent(AtomicReference<Object> event, long sequence, boolean endOfBatch) throws Exception {
        try {
            while (!semaphore.tryAcquire(1, TimeUnit.SECONDS) && !result.isDone()) {
                if (session == null || !session.isOpen())
                    return;
            }

            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write event data
            try {
                Object item = event.get();
                eventWriter.writeTo(item, outputStream);
            } catch (Throwable t) {
                logger.error(marker, "Could not send event", t);
                subscription.cancel();
            }

            // Send it
            ByteBuffer eventBuffer = outputStream.takeByteBuffer();

            session.getRemote().sendBytes(eventBuffer, new WriteCallback() {
                @Override
                public void writeFailed(Throwable t) {
                    pool.release(eventBuffer);
                    onWebSocketError(t);
                }

                @Override
                public void writeSuccess() {
                    sentBytes.mark(eventBuffer.position());
                    pool.release(eventBuffer);
                }
            });

            sent.mark();
            if (endOfBatch) {
                session.getRemote().flush();
                sent.mark(batchSize);
                sentBatchSize.update(batchSize);
            }
        } catch (Throwable e) {
            onError(new ClientStreamException("Failed to send", e));
        }
    }

    @Override
    public void onBatchStart(long batchSize) {
        this.batchSize = batchSize;
    }
}
