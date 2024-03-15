package com.exoreaction.xorcery.reactivestreams.server.reactor;

import com.exoreaction.xorcery.lang.Exceptions;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

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
        implements WebSocketListener {

    private final static JsonMapper jsonMapper = new JsonMapper();

    private final String path;
    private final Map<String, String> publisherParameters;

    private final MessageWriter<OUTPUT> writer;
    private final MessageReader<INPUT> reader;
    private final Publisher<OUTPUT> publisher;
    private FluxSink<INPUT> sink;

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
            Map<String, String> publisherParameters,

            MessageWriter<OUTPUT> writer,
            MessageReader<INPUT> reader,
            Function<Flux<INPUT>, Publisher<OUTPUT>> customizer,

            ByteBufferPool byteBufferPool,
            Logger logger,
            Tracer tracer,
            Meter meter,
            io.opentelemetry.context.Context requestContext) {
        this.path = path;
        this.publisherParameters = publisherParameters;
        this.writer = writer;
        this.reader = reader;
        this.byteBufferPool = byteBufferPool;
        this.tracer = tracer;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(path);
        this.requestContext = requestContext;
        this.outputStream = new ByteBufferOutputStream2(byteBufferPool, false);

        Flux<INPUT> source = Flux.<INPUT>create(sink -> {
            ServerWebSocketStream.this.sink = sink;
//            sink.onCancel(this::cancel);
//            sink.onDispose(this::cancel);
        }).doOnError(throwable ->
        {
            System.out.println(throwable);
        });
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

    private void subscriberRequested(long requested) {
        if (reader != null) {
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
        if (writer != null)
        {
            try {
                writer.writeTo(item, outputStream);
                ByteBuffer eventBuffer = outputStream.takeByteBuffer();
                session.getRemote().sendBytes(eventBuffer);
                byteBufferPool.release(eventBuffer);

                sentBytes.record(eventBuffer.limit(), attributes);
            } catch (IOException e) {
                session.close(StatusCode.NORMAL, getError(e).toPrettyString());
            }
        } else
        {
            upstream().request(1);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (session.isOpen()) {
            session.close(StatusCode.NORMAL, null);
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        if (session.isOpen()) {
            session.close(StatusCode.NORMAL, getError(throwable).toPrettyString());
        }
    }

    @Override
    protected void hookOnCancel() {
        sendRequests(Long.MIN_VALUE);
    }

    // WebSocketListener
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.AUTO);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        try {
            JsonNode json = jsonMapper.readTree(message);
            if (json instanceof NumericNode numericNode) {
                if (upstream() == null) {
                    // Subscribe upstream
                    publisher.subscribe(this);
                }
                long requests = numericNode.asLong();
                if (requests == Long.MIN_VALUE) {
                    upstream().cancel();
                } else if (requests == -1) {
                    if (sink != null)
                        sink.complete();
                } else {
                    upstream().request(requests);
                }
            } else if (json instanceof ObjectNode objectNode) {
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
                contextMap.putAll(publisherParameters);
                context = reactor.util.context.Context.of(contextMap);

                // Subscribe upstream
                publisher.subscribe(this);

                if (reader != null) {
                    // Send complete context back to client
                    sink.contextView().forEach((k, v) ->
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
                    session.getRemote().sendString(contextJsonString);
                    logger.debug(marker, "Sent context {}: {}", session.getRemote().getRemoteAddress().toString(), contextJsonString);

                    context = reactor.util.context.Context.of(sink.contextView());

                    sink.onRequest(this::subscriberRequested);
                }
            }
        } catch (Throwable e) {
            if (sink != null)
                sink.error(e);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (reader == null) {
            if (!redundancyNotificationIssued) {
                logger.warn(marker, "Receiving redundant results from server");
                redundancyNotificationIssued = true;
            }
        } else {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(marker, "onWebSocketBinary {}", new String(payload, offset, len, StandardCharsets.UTF_8));
                }
                INPUT event = reader.readFrom(payload, offset, len);
                sink.next(event);
                outstandingRequests.decrementAndGet();
                sendRequests(0);
            } catch (Throwable e) {
                logger.error("Could not receive value", e);
                if (upstream() != null)
                    upstream().cancel();
                if (sink != null)
                    sink.error(e);
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
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        if (statusCode == StatusCode.NORMAL) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            upstream().cancel();
            if (sink != null)
                sink.complete();
        } else if (statusCode == StatusCode.SHUTDOWN || statusCode == StatusCode.NO_CLOSE) {

            try {
                JsonNode reasonJson = jsonMapper.readTree(reason);
                if (reasonJson instanceof ObjectNode objectNode) {
                    Throwable throwable;
                    String message = objectNode.path("reason").asText();

                    throwable = switch (reasonJson.get("status").asInt()) {
                        case 401 -> new NotAuthorizedStreamException(message);

                        default -> new ServerStreamException(message);
                    };

                    sink.error(throwable);
                } else {
                    sink.error(new ServerStreamException(reason));
                }
            } catch (JsonProcessingException e) {
                sink.error(new ServerStreamException(reason));
            }
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
        }
        span.end();
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
            if (n == 0) {
                rn = requests.get();
                if (rn >= sendRequestsThreshold && outstandingRequests.get() < sendRequestsThreshold) {
                    requests.addAndGet(-sendRequestsThreshold);
                    rn = sendRequestsThreshold;
                    outstandingRequests.addAndGet(rn);
                } else {
                    return;
                }
            } else if (n != Long.MIN_VALUE) {
                rn = requests.addAndGet(n);

                if (sendRequestsThreshold == 0) {
                    sendRequestsThreshold = Math.min((rn * 3) / 4, 2048);
                } else {
                    if (rn < sendRequestsThreshold) {
                        return; // Wait until we have more requests lined up
                    }
                }

                if (rn > sendRequestsThreshold) {
                    requests.addAndGet(-sendRequestsThreshold);
                    rn = sendRequestsThreshold;
                } else {
                    requests.set(0);
                }
                outstandingRequests.addAndGet(rn);
            }

            String requestString = Long.toString(rn);
            session.getRemote().sendString(requestString);

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}", rn);
        } catch (IOException e) {
            logger.error("Could not send requests", e);
            if (upstream() != null)
                upstream().cancel();
            if (sink != null)
                sink.error(e);
        } finally {
            sendLock.unlock();
        }
    }
}
