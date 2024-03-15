package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry;
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
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.exoreaction.xorcery.lang.Exceptions.isCausedBy;
import static com.exoreaction.xorcery.lang.Exceptions.unwrap;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class ClientWebSocketStream<OUTPUT, INPUT>
        extends BaseSubscriber<OUTPUT>
        implements WebSocketListener {

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final static JsonMapper jsonMapper = new JsonMapper();

    private final URI serverUri;
    private final String contentType;

    private final MessageWriter<OUTPUT> writer;
    private final MessageReader<INPUT> reader;
    private final Publisher<OUTPUT> publisher;
    private final FluxSink<INPUT> sink;

    private final WebSocketClientOptions options;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;

    private final Logger logger;
    private final Marker marker;
    private final Span span;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;

    protected final ByteBufferPool byteBufferPool;

    private volatile boolean redundancyNotificationIssued = false;

    // All the state which requires synchronized access to session
    private Iterator<URI> uriIterator;
    private volatile Session session;
    private volatile reactor.util.context.Context context = reactor.util.context.Context.empty();

    private final ByteBufferOutputStream2 outputStream;

    // Requests
    private final Lock sendLock = new ReentrantLock();
    private volatile long sendRequestsThreshold = 0;
    private final AtomicLong requests = new AtomicLong(0);
    private final AtomicLong outstandingRequests = new AtomicLong(0);

    private final Attributes attributes;
    private final LongHistogram sentBytes;
    private final LongHistogram requestsHistogram;

    public ClientWebSocketStream(
            URI serverUri,
            String contentType,

            MessageWriter<OUTPUT> writer,
            MessageReader<INPUT> reader,
            Publisher<OUTPUT> publisher,
            FluxSink<INPUT> sink,

            WebSocketClientOptions options,
            DnsLookup dnsLookup,
            WebSocketClient webSocketClient,

            ByteBufferPool byteBufferPool,
            Meter meter,
            Tracer tracer,
            TextMapPropagator textMapPropagator,
            Logger logger) {
        this.serverUri = serverUri;
        this.contentType = contentType;
        this.reader = reader;
        this.publisher = publisher;
        this.sink = sink;
        this.options = options;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.writer = writer;
        this.byteBufferPool = byteBufferPool;
        this.tracer = tracer;
        this.textMapPropagator = textMapPropagator;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(serverUri.toASCIIString());
        this.outputStream = new ByteBufferOutputStream2(byteBufferPool, false);

        this.attributes = Attributes.builder()
                .put(SemanticAttributes.MESSAGING_SYSTEM, ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM)
                .put(SemanticAttributes.URL_FULL, serverUri.toASCIIString())
                .build();
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();

        span = tracer.spanBuilder(serverUri.toASCIIString() + " client")
                .setSpanKind(SpanKind.CLIENT)
                .setAllAttributes(attributes)
                .startSpan();

        sink.onCancel(this::subscriptionCancel);
        sink.onDispose(this::subscriptionCancel);

        try (Scope scope = span.makeCurrent()) {
            start();
        }
    }

    private void subscriptionRequest(long requested) {
        sendRequests(requested);
    }

    private void subscriptionCancel() {
        sendRequests(Long.MIN_VALUE);
    }

    // Connection process
    public void start() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "start");
        }

        if (!webSocketClient.isStarted()) {
            throw new IllegalStateException("WebSocketClient not started");
        }

        if (serverUri.getScheme().equals("srv")) {
            logger.debug(marker, "Resolving " + serverUri);
            dnsLookup.resolve(serverUri).thenApply(list ->
            {
                this.uriIterator = list.iterator();
                return uriIterator;
            }).thenApply(this::connect).join();
        } else {
            this.uriIterator = List.of(serverUri).iterator();
            connect(uriIterator).join();
        }
    }

    private CompletableFuture<Void> connect(Iterator<URI> subscriberURIs) {
        CompletableFuture<Void> failure = null;
        while (subscriberURIs.hasNext()) {
            URI subscriberWebsocketUri = subscriberURIs.next();

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", subscriberWebsocketUri);
            }

            logger.debug(marker, "Trying " + subscriberWebsocketUri);
            Span connectSpan = tracer.spanBuilder(subscriberWebsocketUri.toASCIIString() + " connect")
                    .setSpanKind(SpanKind.PRODUCER)
                    .startSpan();
            try (Scope scope = connectSpan.makeCurrent()) {
                ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
                clientUpgradeRequest.setHeaders(options.headers());
                clientUpgradeRequest.setCookies(options.cookies());
                clientUpgradeRequest.setSubProtocols(options.subProtocols());
                clientUpgradeRequest.setExtensions(options.extensions().stream().map(ExtensionConfig::parse).toList());
                clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), contentType);
                textMapPropagator.inject(Context.current(), clientUpgradeRequest, jettySetter);
                return webSocketClient.connect(this, subscriberWebsocketUri, clientUpgradeRequest)
                        .thenRun(connectSpan::end).toCompletableFuture();
            } catch (Throwable e) {
                failure = CompletableFuture.failedFuture(unwrap(e));
            }
        }

        if (failure == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("No servers found for URI:" + serverUri));
        } else {
            return failure;
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
        try {
            writer.writeTo(item, outputStream);
            ByteBuffer eventBuffer = outputStream.takeByteBuffer();
            session.getRemote().sendBytes(eventBuffer);
            byteBufferPool.release(eventBuffer);

            sentBytes.record(eventBuffer.limit(), attributes);
        } catch (IOException e) {

            if (isCausedBy(e, EofException.class, ClosedChannelException.class))
            {
                // Shutting down, ignore this
                return;
            }

            sink.error(e);
        }

        if (reader == null) {
            sink.next((INPUT) item);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (session.isOpen()) {
            sendRequests(-1);
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        if (session.isOpen()) {
            sink.error(throwable);
            session.close(StatusCode.SHUTDOWN, "error");
        }
    }

    @Override
    protected void hookOnCancel() {
        sendRequests(Long.MIN_VALUE);
    }

    // WebSocketListener
    @Override
    public void onWebSocketConnect(Session session) {

        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.AUTO);

        ObjectNode contextJson = JsonNodeFactory.instance.objectNode();
        sink.contextView().forEach((k, v) ->
        {
            if (v instanceof String str) {
                contextJson.set(k.toString(), contextJson.textNode(str));
            } else if (v instanceof Long nr) {
                contextJson.set(k.toString(), contextJson.numberNode(nr));
            } else if (v instanceof Double nr) {
                contextJson.set(k.toString(), contextJson.numberNode(nr));
            } else if (v instanceof Boolean bool) {
                contextJson.set(k.toString(), contextJson.booleanNode(bool));
            }
        });

        try {
            String contextJsonString = jsonMapper.writeValueAsString(contextJson);
            session.getRemote().sendString(contextJsonString);
            logger.debug(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());

            sink.onRequest(this::subscriptionRequest);
        } catch (Throwable e) {
            sink.error(e);
            session.close(StatusCode.SERVER_ERROR, getError(e).toPrettyString());
        }
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
                if (requests == Long.MIN_VALUE) {
                    upstream().cancel();
                    sink.complete();
                    session.close(StatusCode.NORMAL, "cancel");
                } else {
                    if (upstream() != null)
                        upstream().request(requests);
                    else
                        logger.warn("Not subscribed to upstream yet!");
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
                context = reactor.util.context.Context.of(contextMap);
                publisher.subscribe(this);
            }
        } catch (Throwable e) {

            if (unwrap(e) instanceof EofException)
                return;

            upstream().cancel();
            sink.error(e);
            session.close(StatusCode.SERVER_ERROR, getError(e).toPrettyString());
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
            } catch (IOException e) {
                logger.error("Could not receive value", e);
                upstream().cancel();
                sink.error(e);
                session.close(StatusCode.BAD_PAYLOAD, getError(e).toPrettyString());
            }
        }
    }

    @Override
    public void onWebSocketError(Throwable throwable) {

        Throwable unwrap = unwrap(throwable);
        if (unwrap instanceof ClosedChannelException || throwable instanceof EofException)
            return;

        if (logger.isDebugEnabled()) {
            logger.debug(marker, "onWebSocketError", throwable);
        }
        if (upstream() != null)
            upstream().cancel();
        if (sink != null)
            sink.error(throwable);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        if (statusCode == StatusCode.NORMAL) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            if (reason == null)
            {
                if (upstream() != null)
                    upstream().cancel();
                if (sink != null)
                    sink.complete();
            } else
            {
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
            }
        } else if (statusCode == StatusCode.SHUTDOWN || statusCode == StatusCode.NO_CLOSE) {
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
            sink.error(new ServerStreamException(reason));
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

        StringWriter exceptionWriter = new StringWriter();
        try (PrintWriter out = new PrintWriter(exceptionWriter)) {
            throwable.printStackTrace(out);
        }
        errorJson.set("exception", errorJson.textNode(exceptionWriter.toString()));
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
            } else if (n == -1) {
                // Complete
            } else if (n != Long.MIN_VALUE) {
                rn = requests.addAndGet(n);

                if (sendRequestsThreshold == 0) {
                    sendRequestsThreshold = Math.max(1, Math.min((rn * 3) / 4, 2048));
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

            if (rn == 0)
            {
                return;
            }
            String requestString = Long.toString(rn);
            session.getRemote().sendString(requestString);

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}", rn);
        } catch (ClosedChannelException e) {
            // Ignore
        } catch (IOException e) {

            if (e.getCause() instanceof EofException)
                return;

            logger.error("Could not send requests", e);
            if (upstream() != null)
                upstream().cancel();
            if (sink != null)
                sink.error(e);
            if (session.isOpen())
                session.close(StatusCode.SERVER_ERROR, e.getMessage());
        } finally {
            sendLock.unlock();
        }
    }
}
