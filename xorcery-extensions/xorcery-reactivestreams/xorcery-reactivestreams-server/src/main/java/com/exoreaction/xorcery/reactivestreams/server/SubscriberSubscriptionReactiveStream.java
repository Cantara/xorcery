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
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SchemaUrls;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.SUBSCRIBER_IO;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.SUBSCRIBER_REQUESTS;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class SubscriberSubscriptionReactiveStream
        extends ServerReactiveStream
        implements Session.Listener.AutoDemanding,
        Subscription {
    private final String streamName;

    private final Function<Configuration, Subscriber<Object>> subscriberFactory;
    private String configurationMessage = "";
    private Configuration subscriberConfiguration;

    protected Subscriber<Object> subscriber;
    protected final MessageReader<Object> eventReader;

    protected final ByteBufferPool byteBufferPool;

    protected final Marker marker;
    protected final Logger logger;

    protected final ObjectMapper objectMapper;

    protected Session session;

    // Subscription
    private final AtomicBoolean isCancelled = new AtomicBoolean();

    private long sendRequestsThreshold = 0; // Don't send requests unless we have this many
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong outstandingRequests = new AtomicLong();
    private final Lock sendLock = new ReentrantLock();

    private final ActiveSubscriptions activeSubscriptions;
    private ActiveSubscriptions.ActiveSubscription activeSubscription;

    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private Span span;
    protected final LongHistogram receivedBytes;
    protected final LongHistogram requestsHistogram;

    public SubscriberSubscriptionReactiveStream(String streamName,
                                                Function<Configuration, Subscriber<Object>> subscriberFactory,
                                                MessageReader<Object> eventReader,
                                                ObjectMapper objectMapper,
                                                ByteBufferPool byteBufferPool,
                                                OpenTelemetry openTelemetry,
                                                Logger logger,
                                                ActiveSubscriptions activeSubscriptions) {
        this.streamName = streamName;
        this.subscriberFactory = subscriberFactory;
        this.eventReader = eventReader;
        this.objectMapper = objectMapper;

        this.byteBufferPool = byteBufferPool;
        this.marker = MarkerManager.getMarker(streamName);
        this.logger = logger;
        this.activeSubscriptions = activeSubscriptions;

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        this.attributes = Attributes.builder()
                .put(MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME, streamName)
                .put(MessagingIncubatingAttributes.MESSAGING_SYSTEM, ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM)
                .build();
        this.receivedBytes = meter.histogramBuilder(SUBSCRIBER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(SUBSCRIBER_REQUESTS)
                .setUnit("{request}").ofLongs().build();

        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    // Subscription
    @Override
    public void request(long n) {
/*
        if (logger.isTraceEnabled())
            logger.trace(marker, "request {}", n);
*/

        sendRequests(n);
    }

    @Override
    public void cancel() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "cancel");

        isCancelled.set(true);
        sendRequests(0);
    }

    // WebSocket

    @Override
    public void onWebSocketOpen(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteSocketAddress());

        this.session = session;

        Optional.ofNullable(session.getUpgradeRequest().getParameterMap().get("configuration")).map(l -> l.get(0)).ifPresent(this::applyConfiguration);

        Context context = textMapPropagator.extract(Context.current(), session.getUpgradeRequest(), jettyGetter);

        span = tracer.spanBuilder(streamName + " subscriber")
                .setParent(context)
                .setSpanKind(SpanKind.PRODUCER)
                .setAllAttributes(attributes)
                .startSpan();
    }

    @Override
    public void onWebSocketPartialText(String message, boolean fin) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketPartialText {} {}", message, fin);

        if (subscriberConfiguration == null) {
            configurationMessage += message;

            if (fin) {
                // Read JSON parameters
                applyConfiguration(configurationMessage);
            }
        }
    }

    private void applyConfiguration(String configuration) {
        try {
            ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(configuration);
            subscriberConfiguration = new Configuration.Builder(configurationJson)
                    .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                    .build();
        } catch (JsonProcessingException e) {
            logger.error("Could not parse subscriber configuration", e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage(), Callback.NOOP);
        }

        try {
            subscriber = subscriberFactory.apply(subscriberConfiguration);
            activeSubscription = new ActiveSubscriptions.ActiveSubscription(streamName, new AtomicLong(), new AtomicLong(), subscriberConfiguration);
            activeSubscriptions.addSubscription(activeSubscription);

            subscriber.onSubscribe(this);

            logger.debug(marker, "Connected to {}", session.getRemoteSocketAddress());
        } catch (Throwable e) {
            // TODO Send exception here
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", Charset.defaultCharset().decode(payload.asReadOnlyBuffer()).toString());
            }
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(payload);
            Object event = eventReader.readFrom(inputStream);
            receivedBytes.record(payload.position(), attributes);
            subscriber.onNext(event);
            activeSubscription.requested().decrementAndGet();
            activeSubscription.received().incrementAndGet();
            outstandingRequests.decrementAndGet();
            callback.succeed();
        } catch (Throwable e) {
            logger.error("Could not receive value", e);
            subscriber.onError(e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage(), callback);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
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
    public void onWebSocketClose(int statusCode, String reason) {
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
            span.end();
        } catch (Exception e) {
            logger.warn(marker, "Could not close subscription sink", e);
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
            session.sendText(requestString, Callback.NOOP);

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}", rn);

        } finally {
            sendLock.unlock();
        }
    }
}
