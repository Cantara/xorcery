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
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

/**
 * @author rickardoberg
 */
public class PublishReactiveStream
        implements WebSocketListener,
        WriteCallback,
        Subscriber<Object> {

    private final static Logger logger = LogManager.getLogger(PublishReactiveStream.class);
    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final WebSocketClient webSocketClient;
    private final DnsLookup dnsLookup;
    protected final ByteBufferPool pool;
    protected final MessageWriter<Object> eventWriter;

    private final ClientConfiguration publisherConfiguration;
    private final Supplier<Configuration> subscriberConfiguration;
    private final ActiveSubscriptions activeSubscriptions;
    protected final CompletableFuture<Void> result;
    protected final Marker marker;
    protected final Span span;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;

    private boolean redundancyNotificationIssued = false;

    private final URI serverUri;
    private final String streamName;
    private final Publisher<Object> publisher;

    // All the state which requires synchronized access to session
    protected Session session;
    private Iterator<URI> uriIterator;
    protected Subscription subscription;

    private final BlockingArrayQueue<Object> sendQueue = new BlockingArrayQueue<>(4096, 1024);
    private final AtomicBoolean isDraining = new AtomicBoolean();
    private final Lock drainLock = new ReentrantLock();

    protected final ByteBufferOutputStream2 outputStream;

    protected AtomicBoolean isComplete = new AtomicBoolean();
    protected AtomicBoolean isReconnecting = new AtomicBoolean();
    protected AtomicBoolean isRetrying = new AtomicBoolean();
    private long retryDelay;
    private ActiveSubscriptions.ActiveSubscription activeSubscription;

    protected final Attributes attributes;
    protected final LongHistogram sentBytes;
    protected final LongHistogram requestsHistogram;
    protected final LongHistogram flushHistogram;

    public PublishReactiveStream(URI serverUri,
                                 String streamName,
                                 ClientConfiguration publisherConfiguration,
                                 DnsLookup dnsLookup,
                                 WebSocketClient webSocketClient,
                                 Publisher<Object> publisher,
                                 MessageWriter<Object> eventWriter,
                                 Supplier<Configuration> subscriberConfiguration,
                                 ByteBufferPool pool,
                                 OpenTelemetry openTelemetry,
                                 ActiveSubscriptions activeSubscriptions,
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
        this.activeSubscriptions = activeSubscriptions;
        this.result = result;
        this.retryDelay = Duration.parse("PT" + publisherConfiguration.getRetryDelay()).toMillis();
        this.marker = MarkerManager.getMarker(serverUri.getAuthority() + "/" + streamName);
        this.outputStream = new ByteBufferOutputStream2(pool, false);

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
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();

        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();

        // Client completed
        result.whenComplete(this::resultComplete);

        span = tracer.spanBuilder(streamName + " publisher")
                .setSpanKind(SpanKind.PRODUCER)
                .setAllAttributes(attributes)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            start();
        }
    }

    private void resultComplete(Void result, Throwable throwable) {
        isComplete.set(true);
        span.end();
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

    private synchronized void connect(Iterator<URI> subscriberURIs) {
        if (subscriberURIs.hasNext()) {
            URI subscriberWebsocketUri = subscriberURIs.next();

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", subscriberWebsocketUri);
            }

            String queryParameters = "?configuration=" + URLEncoder.encode(subscriberConfiguration.get().json().toString(), StandardCharsets.UTF_8);
            URI effectiveSubscriberWebsocketUri = URI.create(subscriberWebsocketUri.getScheme() + "://" + subscriberWebsocketUri.getAuthority() + "/streams/subscribers/" + streamName + queryParameters);

            logger.debug(marker, "Trying " + effectiveSubscriberWebsocketUri);
            Span connectSpan = tracer.spanBuilder(streamName + " connect")
                    .setSpanKind(SpanKind.PRODUCER)
                    .startSpan();
            try (Scope scope = connectSpan.makeCurrent()) {
                ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
                textMapPropagator.inject(Context.current(), clientUpgradeRequest, jettySetter);
                publisherConfiguration.getExtensions().forEach(clientUpgradeRequest::addExtensions);
                webSocketClient.connect(this, effectiveSubscriberWebsocketUri, clientUpgradeRequest)
                        .thenAccept(this::connected)
                        .exceptionally(this::connectException)
                        .thenRun(connectSpan::end);
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + effectiveSubscriberWebsocketUri.toASCIIString(), e);
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
        this.isReconnecting.set(false);
        this.isRetrying.set(false);
    }

    private Void connectException(Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "connectException", throwable);
        }
        this.isRetrying.set(false);
        retry(throwable);
        return null;
    }

    // Subscriber
    // These calls come from the upstream client publisher
    @Override
    public void onSubscribe(Subscription subscription) {
        // Session is already synchronized by onWebsocketText
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onSubscribe");
        }
        this.subscription = subscription;
    }

    @Override
    public void onNext(Object item) {
        if (logger.isTraceEnabled()) {
//            logger.trace(marker, "onNext {}", item.toString());
        }

        while (isReconnecting.get()) {
            if (isComplete.get())
                return; // Give up
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return; // Give up
            }
        }

        if (!sendQueue.offer(item)) {
            logger.error(marker, "Could not put item on queue {}/{}", sendQueue.getCapacity(), sendQueue.getMaxCapacity());
        }

        if (!isDraining.get()) {
            CompletableFuture.runAsync(this::drainJob);
        }
        activeSubscription.requested().decrementAndGet();
    }

    @Override
    public void onError(Throwable throwable) {
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

    public void onComplete() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onComplete");
        }

        isComplete.set(true);
        if (!isDraining.get()) {
            CompletableFuture.runAsync(this::drainJob);
        }
        logger.debug(marker, "Waiting for outstanding events to be sent to {}", session.getRemote().getRemoteAddress());
    }

    // Whoever calls this should have a synchronized on the session, if available
    public void retry(Throwable cause) {

        if (isComplete.get()) {
            if (!result.isDone() && !isDraining.get()) {
                result.complete(null);
            }
        }

        if (result.isDone() || isRetrying.get()) {
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(marker, "retry");
        }

        if (publisherConfiguration.isRetryEnabled() && publisherConfiguration.isRetryable(cause)) {

            if (!isReconnecting.get()) {
                logger.debug(marker, "Reconnecting");
                isReconnecting.set(true);
            }

            if (uriIterator.hasNext()) {
                connect(uriIterator);
            } else {
                if (cause instanceof ServerShutdownStreamException) {
                    retryDelay = 0;
                }
                isRetrying.set(true);
                logger.trace(marker, "Retrying in {}s", retryDelay / 1000);
                CompletableFuture.delayedExecutor(retryDelay, TimeUnit.MILLISECONDS).execute(this::start);
                // Exponential backoff, gets reset on successful connect, max 60s delay
                retryDelay = Math.min(retryDelay * 2, 60000);
            }
        } else {
            isComplete.set(true);
            if (subscription != null) {
                subscription.cancel();
            }

            if (cause == null)
                result.complete(null);
            else
                result.completeExceptionally(cause);
        }
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {

        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }

        if (isRetrying.get()) {
            logger.debug(marker, "Reconnected");
            isRetrying.set(false);
        }

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
        this.retryDelay = Duration.parse("PT" + publisherConfiguration.getRetryDelay()).toMillis();

        Configuration configuration = subscriberConfiguration.get();
        activeSubscription = new ActiveSubscriptions.ActiveSubscription(streamName, new AtomicLong(), new AtomicLong(), configuration);
        activeSubscriptions.addSubscription(activeSubscription);

        // First send parameters, if available
/*
        String parameterString = configuration.json().toPrettyString();

        try {
            session.getRemote().sendString(parameterString);
            session.getRemote().flush();
            logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
        } catch (Throwable t) {
            session.close(StatusCode.SERVER_ERROR, t.getMessage());
            logger.error(marker, "Parameter handshake failed", t);
        }
*/
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        long requestAmount = Long.parseLong(message);

        if (requestAmount == Long.MIN_VALUE) {
            logger.debug(marker, "Received cancel on websocket");
            session.close(StatusCode.NORMAL, "cancel");

            // Maybe try another subscriber
            retry(new ServerShutdownStreamException("cancel"));
        } else {
            if (subscription == null) {
                publisher.subscribe(this);
            }

            requestsHistogram.record(requestAmount, attributes);
            activeSubscription.requested().addAndGet(requestAmount);
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
    public void onWebSocketError(Throwable cause) {

        if (cause instanceof ClosedChannelException) {
            // Ignore
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(marker, "onWebSocketError", cause);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        activeSubscriptions.removeSubscription(activeSubscription);
        activeSubscription = null;
        if (statusCode == StatusCode.NORMAL) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            result.complete(null);
        } else if (statusCode == StatusCode.SHUTDOWN || statusCode == StatusCode.NO_CLOSE) {
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
            retry(new ServerShutdownStreamException(reason));
        }
    }

    private void drainJob() {
        drainLock.lock();
        isDraining.set(true);
        try {
            if (activeSubscription != null) {
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
            }
        } catch (Throwable t) {
            logger.error(marker, "Could not send event", t);
            if (session.isOpen()) {
                session.close(StatusCode.SERVER_ERROR, t.getMessage());
            }
        } finally {
            logger.trace(marker, "Stop drain");

            isDraining.set(false);
            drainLock.unlock();
        }

        if (!sendQueue.isEmpty())
        {
            // Race conditions suck. Need to figure out a better way to handle this...
            drainJob();
        } else
        {
            checkDone();
        }
    }

    protected void send(Object item) throws IOException {
//        if (logger.isTraceEnabled())
//            logger.trace(marker, "send {}", item.getClass().toString());

        // Write event data
        writeItem(eventWriter, item, outputStream);
        ByteBuffer eventBuffer = outputStream.takeByteBuffer();
        sentBytes.record(eventBuffer.limit(), attributes);
        session.getRemote().sendBytes(eventBuffer);
        pool.release(eventBuffer);
    }

    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }

    protected void checkDone() {
        if (isComplete.get()) {
            if (session != null && session.isOpen()) {
                logger.debug(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
                session.close(StatusCode.NORMAL, "complete");

                if (!result.isDone()) {
                    result.complete(null);
                }
            }
        }
    }
}
