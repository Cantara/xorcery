package com.exoreaction.xorcery.reactivestreams.server.reactor;

import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.lang.Exceptions;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class ServerWebSocketStream<OUTPUT, INPUT>
        extends BaseSubscriber<OUTPUT>
        implements Session.Listener.AutoDemanding {

    private final static JsonMapper jsonMapper = new JsonMapper();

    private final static long CANCEL = Long.MIN_VALUE;
    private final static long COMPLETE = -1L;
    private final static long SEND_BUFFERED_REQUESTS = 0;

    private final String path;
    private final Map<String, String> pathParameters;

    private final ServerWebSocketOptions options;
    private final MessageWriter<OUTPUT> writer;
    private final MessageReader<INPUT> reader;
    private final Publisher<OUTPUT> publisher;
    private FluxSink<INPUT> downstreamSink;

    private final Logger logger;
    private final Marker marker;
    private final Tracer tracer;
    private final Attributes attributes;
    private final LongHistogram sentBytes;
    private final LongHistogram requestsHistogram;
    private final LongHistogram flushHistogram;
    private final Context requestContext;
    private final Span span;

    private final ByteBufferPool byteBufferPool;

    private volatile boolean redundancyNotificationIssued = false;

    private volatile Session session;
    private volatile reactor.util.context.Context context = reactor.util.context.Context.empty();

    private final ByteBufferOutputStream2 outputStream;

    // Requests
    private final Lock sendLock = new ReentrantLock();
    private volatile long sendRequestsThreshold = 0;
    private final AtomicLong requests = new AtomicLong(0);
    private final AtomicLong outstandingRequests = new AtomicLong(0);

    public ServerWebSocketStream(
            String path,
            Map<String, String> pathParameters,
            ServerWebSocketOptions options,

            MessageWriter<OUTPUT> writer,
            MessageReader<INPUT> reader,
            Function<Flux<INPUT>, Publisher<OUTPUT>> customizer,

            ByteBufferPool byteBufferPool,
            Logger logger,
            Tracer tracer,
            Meter meter,
            io.opentelemetry.context.Context requestContext) {
        this.path = path;
        this.pathParameters = pathParameters;
        this.options = options;
        this.writer = writer;
        this.reader = reader;
        this.byteBufferPool = byteBufferPool;
        this.tracer = tracer;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(path);
        this.requestContext = requestContext;
        this.outputStream = new ByteBufferOutputStream2(byteBufferPool, false);

        Flux<INPUT> source = Flux.<INPUT>create(sink -> {
            ServerWebSocketStream.this.downstreamSink = sink;
            sink.onCancel(this::upstreamCancel);
// TODO Do we need to handle this? sink.onDispose(someCallback);
        }).publishOn(Schedulers.boundedElastic(), 2048); // TODO This needs to be configurable
        this.publisher = customizer.apply(source);

        this.attributes = Attributes.builder()
                .put(SemanticAttributes.MESSAGING_DESTINATION_NAME, path)
                .put(SemanticAttributes.MESSAGING_SYSTEM, XORCERY_MESSAGING_SYSTEM)
                .build();
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();
        this.span = tracer.spanBuilder(path + " server")
                .setParent(requestContext)
                .setSpanKind(SpanKind.SERVER)
                .setAllAttributes(attributes)
                .startSpan();
    }

    private void upstreamCancel() {
        sendRequests(Long.MIN_VALUE);
    }

    private void subscriberRequested(long requested) {
        if (reader != null && requested > 0) {
            sendRequests(requested);
        }
    }

    // Subscriber
    @Override
    public reactor.util.context.Context currentContext() {
        return context;
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        // No-op
    }

    @Override
    protected void hookOnNext(OUTPUT item) {
        if (writer != null) {
            try {
                writer.writeTo(item, outputStream);
                RetainableByteBuffer retainableByteBuffer = outputStream.takeByteBuffer();
                ByteBuffer eventBuffer = retainableByteBuffer.getByteBuffer();
                session.sendBinary(eventBuffer, new ReleaseCallback(retainableByteBuffer));
                sentBytes.record(eventBuffer.limit(), attributes);
            } catch (IOException e) {
                session.close(StatusCode.NORMAL, getError(e).toPrettyString(), Callback.NOOP);
            }
        } else {
            upstream().request(1);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (session.isOpen()) {
            session.close(StatusCode.NORMAL, null, Callback.NOOP);
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        if (session.isOpen()) {
            logger.error(marker, "Reactive stream error", throwable);
            session.close(StatusCode.NORMAL, getError(throwable).toPrettyString(), Callback.NOOP);
        }
    }

    // Session.Listener.AutoDemanding
    @Override
    public void onWebSocketOpen(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteSocketAddress());

        this.session = session;
        session.setMaxOutgoingFrames(options.maxOutgoingFrames());
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        try {
            JsonNode json = jsonMapper.readTree(message);
            if (json instanceof NumericNode numericNode) {
                long requests = numericNode.asLong();
                if (requests == COMPLETE) {
                    if (downstreamSink != null)
                        downstreamSink.complete();
                } else {
                    if (upstream() == null) {
                        // Subscribe upstream
                        publisher.subscribe(this);
                    }
                    upstream().request(requests);
                }
            } else if (json instanceof ObjectNode objectNode) {
                // Add downstream context
                Map<String, Object> contextMap = new HashMap<>();
                for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
                    JsonNode value = property.getValue();
                    switch (value.getNodeType()) {
                        case STRING -> contextMap.put(property.getKey(), value.asText());
                        case BOOLEAN -> contextMap.put(property.getKey(), value.asBoolean());
                        case NUMBER ->
                                contextMap.put(property.getKey(), value.isIntegralNumber() ? value.asLong() : value.asDouble());
                    }
                }

                // Add path parameters
                contextMap.putAll(pathParameters);

                // Add query parameters
                session.getUpgradeRequest().getParameterMap().forEach((k, v) -> contextMap.put(k, v.get(0)));

                // Add request/response objects
                contextMap.put("request", session.getUpgradeRequest());
                contextMap.put("response", session.getUpgradeResponse());
                context = reactor.util.context.Context.of(contextMap);

                // Subscribe upstream
                publisher.subscribe(this);

                if (reader != null) {
                    // Send complete context back to client
                    downstreamSink.contextView().forEach((k, v) ->
                    {
                        if (v instanceof String str) {
                            objectNode.set(k.toString(), objectNode.textNode(str));
                        } else if (v instanceof Long nr) {
                            objectNode.set(k.toString(), objectNode.numberNode(nr));
                        } else if (v instanceof Double nr) {
                            objectNode.set(k.toString(), objectNode.numberNode(nr));
                        } else if (v instanceof Boolean bool) {
                            objectNode.set(k.toString(), objectNode.booleanNode(bool));
                        }
                    });

                    String contextJsonString = jsonMapper.writeValueAsString(objectNode);
                    session.sendText(contextJsonString, Callback.NOOP);
                    logger.debug(marker, "Sent context {}: {}", session.getRemoteSocketAddress(), contextJsonString);

                    context = reactor.util.context.Context.of(downstreamSink.contextView());

                    downstreamSink.onRequest(this::subscriberRequested);
                }
            }
        } catch (Throwable e) {
            if (downstreamSink != null)
                downstreamSink.error(e);
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer byteBuffer, Callback callback) {
        if (reader == null) {
            if (!redundancyNotificationIssued) {
                logger.warn(marker, "Receiving redundant results from server");
                redundancyNotificationIssued = true;
            }
            callback.succeed();
        } else {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(marker, "onWebSocketBinary {}", StandardCharsets.UTF_8.decode(byteBuffer.asReadOnlyBuffer()).toString());
                }
                INPUT event = reader.readFrom(new ByteBufferBackedInputStream(byteBuffer));
                downstreamSink.next(event);
                outstandingRequests.decrementAndGet();
                sendRequests(SEND_BUFFERED_REQUESTS);
                callback.succeed();
            } catch (Throwable e) {
                logger.error("Could not receive value", e);
                if (upstream() != null)
                    upstream().cancel();
                if (downstreamSink != null)
                    downstreamSink.error(e);
                callback.fail(e);
            }
        }
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        if (logger.isDebugEnabled() && !Exceptions.isCausedBy(throwable, ClosedChannelException.class)) {
            logger.debug(marker, "onWebSocketError", throwable);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
            } else
            {
                logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            }

            Subscription upstream = upstream();
            if (upstream != null)
                upstream.cancel();
        } finally {
            span.end();
        }
    }

    protected ObjectNode getError(Throwable throwable) {
        ObjectNode errorJson = JsonNodeFactory.instance.objectNode();

        if (throwable instanceof NotAuthorizedStreamException) {
            errorJson.set("status", errorJson.numberNode(HttpStatus.UNAUTHORIZED_401));
        } else {
            errorJson.set("status", errorJson.numberNode(HttpStatus.INTERNAL_SERVER_ERROR_500));
        }

        errorJson.set("reason", errorJson.textNode(throwable.getMessage()));
        return errorJson;
    }

    // Send requests
    protected void sendRequests(long n) {

        if (session == null || !session.isOpen())
            return;

        sendLock.lock();
        try {
            long rn = n;
            if (n == SEND_BUFFERED_REQUESTS) {
                rn = requests.get();
                // TODO: There is something wrong with the logic here, need to figure out exactly what
                if (rn >= sendRequestsThreshold && outstandingRequests.get() < sendRequestsThreshold) {
                    requests.addAndGet(-sendRequestsThreshold);
                    rn = sendRequestsThreshold;
                    outstandingRequests.addAndGet(rn);
                } else {
                    return;
                }
            } else {
                rn = requests.addAndGet(n);

                if (sendRequestsThreshold == 0) {
                    sendRequestsThreshold = Math.min((rn * 3) / 4, 2048);
                } else {
                    if (rn < sendRequestsThreshold) {
                        return; // Wait until we have more requests lined up
                    } else {
                        requests.addAndGet(-sendRequestsThreshold);
                        rn = sendRequestsThreshold;
                    }
                }
                outstandingRequests.addAndGet(rn);
            }

            String requestString = Long.toString(rn);
            session.sendText(requestString, Callback.NOOP);

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}",
                        rn == COMPLETE ? "COMPLETE"
                                : rn);
        } finally {
            sendLock.unlock();
        }
    }

    public record ReleaseCallback(RetainableByteBuffer byteBuffer)
            implements Callback {
        @Override
        public void succeed() {
            byteBuffer.release();
        }

        @Override
        public void fail(Throwable x) {
            byteBuffer.release();
        }
    }
}
