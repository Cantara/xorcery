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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientBadPayloadStreamException;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.common.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.SUBSCRIBER_IO;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.SUBSCRIBER_REQUESTS;

public class SubscribeReactiveStream
        implements WebSocketListener,
        WebSocketConnectionListener,
        Subscription {

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final URI serverUri;
    private final String streamName;

    private final ClientConfiguration subscriberConfiguration;
    private Iterator<URI> uriIterator;
    private long retryDelay;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;
    private final Supplier<Configuration> publisherConfiguration;

    protected final Subscriber<Object> subscriber;
    protected final MessageReader<Object> eventReader;

    protected final Marker marker;
    protected final Logger logger;
    protected final CompletableFuture<Void> result;

    protected Session session;

    private final AtomicBoolean isCancelled = new AtomicBoolean(); // true if cancel() has been called or the result has been completed
    protected final AtomicBoolean isComplete = new AtomicBoolean(); // true if onComplete or onError has been called
    protected final AtomicBoolean isRetrying = new AtomicBoolean(); // true if we're in the process of retrying/reconnecting

    private long sendRequestsThreshold = 0;
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong outstandingRequests = new AtomicLong();

    private final Lock sendLock = new ReentrantLock();
    private final SendWriteCallback sendWriteCallback = new SendWriteCallback();

    private final ActiveSubscriptions activeSubscriptions;
    private ActiveSubscriptions.ActiveSubscription activeSubscription;

    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final Span span;
    protected final Attributes attributes;
    protected final LongHistogram receivedBytes;
    protected final LongHistogram requestsHistogram;


    public SubscribeReactiveStream(URI serverUri,
                                   String streamName,
                                   ClientConfiguration subscriberConfiguration,
                                   DnsLookup dnsLookup,
                                   WebSocketClient webSocketClient,
                                   Subscriber<Object> subscriber,
                                   MessageReader<Object> eventReader,
                                   Supplier<Configuration> publisherConfiguration,
                                   OpenTelemetry openTelemetry,
                                   Logger logger,
                                   ActiveSubscriptions activeSubscriptions,
                                   CompletableFuture<Void> result) {
        this.serverUri = serverUri;
        this.streamName = streamName;
        this.subscriberConfiguration = subscriberConfiguration;
        this.eventReader = eventReader;
        this.retryDelay = Duration.parse("PT" + subscriberConfiguration.getRetryDelay()).toMillis();
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.subscriber = subscriber;
        this.publisherConfiguration = publisherConfiguration;

        this.marker = MarkerManager.getMarker(this.serverUri.getAuthority() + "/" + streamName);
        this.logger = logger;
        this.activeSubscriptions = activeSubscriptions;

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        this.attributes = Attributes.builder()
                .put(SemanticAttributes.MESSAGING_DESTINATION_NAME, streamName)
                .put(SemanticAttributes.MESSAGING_SYSTEM, ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM)
                .put(SemanticAttributes.URL_FULL, serverUri.toASCIIString())
                .build();
        this.receivedBytes = meter.histogramBuilder(SUBSCRIBER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(SUBSCRIBER_REQUESTS)
                .setUnit("{request}").ofLongs().build();

        this.result = result;
        result.whenComplete(this::resultComplete);

        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();

        span = tracer.spanBuilder(streamName + " subscriber")
                .setSpanKind(SpanKind.CONSUMER)
                .setAllAttributes(attributes)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            start();
        }
    }

    private void resultComplete(Void result, Throwable throwable) {
        span.end();
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

        if (serverUri.getScheme().equals("srv")) {
            logger.debug(marker, "Resolving " + serverUri);
            dnsLookup.resolve(serverUri).thenApply(list ->
            {
                this.uriIterator = list.iterator();
                return uriIterator;
            }).thenAccept(this::connect).exceptionally(this::connectException);
        } else {
            this.uriIterator = List.of(serverUri).iterator();
            connect(uriIterator);
        }
    }

    private void connect(Iterator<URI> publisherURIs) {

        if (publisherURIs.hasNext()) {
            URI publisherWebsocketUri = publisherURIs.next();

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", publisherWebsocketUri);
            }

            String queryParameters = "?configuration=" + URLEncoder.encode(publisherConfiguration.get().json().toString(), StandardCharsets.UTF_8);
            URI effectivePublisherWebsocketUri = URI.create(publisherWebsocketUri.getScheme() + "://" + publisherWebsocketUri.getAuthority() + "/streams/publishers/" + streamName + queryParameters);
            logger.debug(marker, "Trying " + effectivePublisherWebsocketUri);
            Span connectSpan = tracer.spanBuilder(streamName + " connect")
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();
            try (Scope scope = connectSpan.makeCurrent()) {
                ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
                subscriberConfiguration.getExtensions().forEach(clientUpgradeRequest::addExtensions);
                textMapPropagator.inject(Context.current(), clientUpgradeRequest, jettySetter);
                webSocketClient.connect(this, effectivePublisherWebsocketUri, clientUpgradeRequest)
                        .thenAccept(this::connected)
                        .exceptionally(this::connectException)
                        .thenRun(connectSpan::end);
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
    }

    private Void connectException(Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "exceptionally", throwable);
        }
        retry(throwable);
        return null;
    }

    public void retry(Throwable cause) {
        if (subscriberConfiguration.isRetryEnabled()) {

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "retry");
            }

            if (!isRetrying.get()) {
                isRetrying.set(true);
            }

            if (uriIterator.hasNext()) {
                connect(uriIterator);
            } else {
                if (cause instanceof ServerTimeoutStreamException) {
                    start();
                } else {
                    logger.debug(marker, String.format("Retrying in %ds", retryDelay / 1000), cause);
                    CompletableFuture.delayedExecutor(retryDelay, TimeUnit.MILLISECONDS).execute(this::start);
                    // Exponential backoff, gets reset on successful connect, max 60s delay
                    retryDelay = Math.min(retryDelay * 2, 60000);
                }
            }
        } else {
            if (cause == null) {
                if (!isComplete.get()) {
                    isComplete.set(true);
                    subscriber.onComplete();
                }
                result.cancel(true);
            } else {
                if (!isComplete.get()) {
                    isComplete.set(true);
                    subscriber.onError(cause);
                }
                result.completeExceptionally(cause);
            }
        }
    }

    // Subscription
    @Override
    public void request(long n) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "request {}", n);
        }

        sendRequests(n);
    }

    @Override
    public void cancel() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "cancel");
        }

        isCancelled.set(true);
        sendRequests(0);
    }

    // Websocket
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }
        this.session = session;
        this.retryDelay = Duration.parse("PT" + subscriberConfiguration.getRetryDelay()).toMillis();

        Configuration configuration = publisherConfiguration.get();
        activeSubscription = new ActiveSubscriptions.ActiveSubscription(streamName, new AtomicLong(), new AtomicLong(), configuration);
        activeSubscriptions.addSubscription(activeSubscription);

        logger.debug(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
        if (isRetrying.get()) {
            isRetrying.set(false);
            long retryRequests = requests.get() + outstandingRequests.get();
            outstandingRequests.set(0);
            requests.set(0);
            request(retryRequests);
        } else {
            subscriber.onSubscribe(this);
        }

        // First send parameters, if available
/*
        String parameterString = configuration.json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAcceptAsync(Void ->
                {
                    logger.debug(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());

                    if (isRetrying.get()) {
                        isRetrying.set(false);
                        long retryRequests = requests.get() + outstandingRequests.get();
                        outstandingRequests.set(0);
                        requests.set(0);
                        request(retryRequests);
                    } else {
                        subscriber.onSubscribe(this);
                    }
                }).exceptionally(t ->
                {
                    session.close(StatusCode.SERVER_ERROR, t.getMessage());
                    logger.error(marker, "Parameter handshake failed", t);
                    return null;
                })
        ));
*/
    }

    @Override
    public void onWebSocketText(String payload) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketPartialText {} {}", payload);
        }

        Throwable throwable;
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(Base64.getDecoder().decode(payload));
            ObjectInputStream oin = new ObjectInputStream(bin);
            throwable = (Throwable) oin.readObject();
        } catch (Throwable e) {
            throwable = e;
        }

        if (!isComplete.get()) {
            isComplete.set(true);
            subscriber.onError(throwable);
        }

        // Ack to publisher
        WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
        session.close(StatusCode.NORMAL, "onError", callback);
        Throwable finalThrowable = throwable;
        callback.future().handle((v, t) -> result.completeExceptionally(finalThrowable));
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", new String(payload, offset, len, StandardCharsets.UTF_8));
            }
//            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(payload, offset, len);
            subscriber.onNext(event);
            receivedBytes.record(len, attributes);
            activeSubscription.requested().decrementAndGet();
            activeSubscription.received().incrementAndGet();
            outstandingRequests.decrementAndGet();
        } catch (IOException e) {
            logger.error("Could not receive value", e);

            if (!isComplete.get()) {
                isComplete.set(true);
                subscriber.onError(e);
            }

            // These kinds of errors are not retryable, we're done
            WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
            session.close(StatusCode.NORMAL, "onError", callback);
            callback.future().handle((v, t) -> result.completeExceptionally(e));
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        cause = unwrap(cause);

        if (!(cause instanceof ClosedChannelException)) {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketError", cause);
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        if (activeSubscription != null) {
            activeSubscriptions.removeSubscription(activeSubscription);
            activeSubscription = null;
        }
        if (statusCode == StatusCode.NORMAL) {
            try {
                if (!isComplete.get()) {
                    isComplete.set(true);
                    subscriber.onComplete();
                }
            } catch (Exception e) {
                logger.warn(marker, "Could not close subscription", e);
            }
            logger.debug(marker, "Complete subscription:{} {}", statusCode, reason);
            result.complete(null); // Now considered done
        } else if (statusCode == StatusCode.BAD_PAYLOAD) {
            try {
                if (!result.isCompletedExceptionally() && !isComplete.get()) {
                    isComplete.set(true);
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
    protected void sendRequests(long n) {
        sendLock.lock();
        try {
            long rn;
            if (isCancelled.get()) {
                rn = Long.MIN_VALUE;
            } else {
                rn = requests.addAndGet(n);

                if (sendRequestsThreshold == 0) {
                    sendRequestsThreshold = Math.min((rn * 3) / 4, 2048);
                } else {
                    if (rn < sendRequestsThreshold) {
    /*
                                if (logger.isTraceEnabled())
                                    logger.trace(marker, "sendRequest not yet {}", rn);
    */
                        return; // Wait until we have more requests lined up
                    }
                }

                requests.set(0);
                outstandingRequests.addAndGet(rn);
                activeSubscription.requested().addAndGet(rn);
                requestsHistogram.record(rn, attributes);
            }

            String requestString = Long.toString(rn);
            CompletableFuture.runAsync(() ->
            {
                session.getRemote().sendString(requestString, sendWriteCallback);
            });

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}", rn);
        } finally {
            sendLock.unlock();
        }
    }

    private class SendWriteCallback
            implements WriteCallback {
        @Override
        public void writeFailed(Throwable x) {
            logger.error(marker, "Could not send requests", x);
        }

        @Override
        public void writeSuccess() {
            // Do nothing
        }
    }

}
