package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.client.ReleaseCallback;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
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
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.exoreaction.xorcery.lang.Exceptions.isCausedBy;
import static com.exoreaction.xorcery.lang.Exceptions.unwrap;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class ClientWebSocketStream<OUTPUT, INPUT>
        extends BaseSubscriber<OUTPUT>
        implements Session.Listener.AutoDemanding {

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final static JsonMapper jsonMapper = new JsonMapper();

    private final static long CANCEL = Long.MIN_VALUE;
    private final static long COMPLETE = -1L;
    private final static long SEND_BUFFERED_REQUESTS = 0;

    private final URI serverUri;
    private final ReactiveStreamSubProtocol subProtocol;
    private final Collection<String> outputContentTypes;
    private final Collection<String> inputContentTypes;
    private final Class<?> outputType;
    private final Class<?> inputType;
    private final MessageWorkers messageWorkers;
//    private final String contentType;

    private final Publisher<OUTPUT> publisher; // if null -> subscribe
    private final FluxSink<INPUT> sink;

    private final ClientWebSocketOptions options;
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
    private MessageWriter<OUTPUT> writer; // if null -> subscribe
    private MessageReader<INPUT> reader; // if null -> publish
    private Iterator<URI> uriIterator;
    private volatile Session session;
    private volatile reactor.util.context.Context context = reactor.util.context.Context.empty();

    private final ByteBufferOutputStream2 outputStream;

    // Requests
    private final Lock sendLock = new ReentrantLock();
    private volatile long sendRequestsThreshold = 0;
    private final AtomicLong requests = new AtomicLong(0);
    private final AtomicLong upstreamRequests = new AtomicLong(0);
    private final AtomicLong outstandingRequests = new AtomicLong(0);
    private volatile Throwable error; // Upstream error we are saving for when server closes the websocket

    private final Attributes attributes;
    private final LongCounter receivedCounter;
    private final LongHistogram receivedBytes;
    private final LongHistogram sentBytes;
    private final LongHistogram requestsHistogram;

    public ClientWebSocketStream(
            URI serverUri,
            ReactiveStreamSubProtocol subProtocol,
            Collection<String> outputContentTypes,
            Collection<String> inputContentTypes,
            Class<?> outputType,
            Class<?> inputType,
            MessageWorkers messageWorkers,
            Publisher<OUTPUT> publisher,
            FluxSink<INPUT> downstreamSink,

            ClientWebSocketOptions options,
            DnsLookup dnsLookup,
            WebSocketClient webSocketClient,

            ByteBufferPool byteBufferPool,
            Meter meter,
            Tracer tracer,
            TextMapPropagator textMapPropagator,
            Logger logger) {
        this.serverUri = serverUri;
        this.subProtocol = subProtocol;
        this.outputContentTypes = outputContentTypes;
        this.inputContentTypes = inputContentTypes;
        this.outputType = outputType;
        this.inputType = inputType;
        this.messageWorkers = messageWorkers;
        this.publisher = publisher;
        this.sink = downstreamSink;
        this.options = options;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
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
        this.receivedCounter = meter.counterBuilder(SUBSCRIBER_REQUESTS)
                .setUnit("{request}").build();
        this.receivedBytes = meter.histogramBuilder(SUBSCRIBER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();

        span = tracer.spanBuilder(serverUri.toASCIIString() + " client")
                .setSpanKind(SpanKind.CLIENT)
                .setAllAttributes(attributes)
                .startSpan();

        downstreamSink.onCancel(this::upstreamCancel);
// TODO Should we handle this callback? downstreamSink.onDispose(someCallback);

        try (Scope scope = span.makeCurrent()) {
            start();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re)
                throw re;
            else if (e.getCause() instanceof IOException ioe) {
                throw new UncheckedIOException(ioe);
            } else {
                throw e;
            }
        }
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

            Span connectSpan = tracer.spanBuilder(subscriberWebsocketUri.toASCIIString() + " connect")
                    .setSpanKind(SpanKind.PRODUCER)
                    .startSpan();
            try (Scope scope = connectSpan.makeCurrent()) {
                ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
                clientUpgradeRequest.setHeaders(options.headers());
                clientUpgradeRequest.setCookies(options.cookies());
                clientUpgradeRequest.setSubProtocols(subProtocol.name());
                clientUpgradeRequest.setExtensions(options.extensions().stream().map(ExtensionConfig::parse).toList());
                switch (subProtocol) {
                    case publisher -> {
                        clientUpgradeRequest.setHeader(HttpHeader.ACCEPT.asString(), List.copyOf(inputContentTypes));
                    }
                    case subscriber -> {
                        clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), List.copyOf(outputContentTypes));
                    }
                    case subscriberWithResult -> {
                        clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), List.copyOf(outputContentTypes));
                        clientUpgradeRequest.setHeader(HttpHeader.ACCEPT.asString(), List.copyOf(inputContentTypes));
                    }
                }
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
        subscription.request(upstreamRequests.getAndSet(0));
    }

    @Override
    protected void hookOnNext(OUTPUT item) {
        try {
            writer.writeTo(item, outputStream);
            RetainableByteBuffer retainableByteBuffer = outputStream.takeByteBuffer();
            ByteBuffer eventBuffer = retainableByteBuffer.getByteBuffer();
            session.sendBinary(eventBuffer, new ReleaseCallback(retainableByteBuffer));
            sentBytes.record(eventBuffer.limit(), attributes);

            // publish without result, tell downstream subscriber the item has been handled
            if (reader == null) {
                sink.next((INPUT) item);
            }
        } catch (IOException e) {

            if (isCausedBy(e, EofException.class, ClosedChannelException.class)) {
                // Shutting down, ignore this
                return;
            }

            // Tell server we have a problem
            onError(e);

            // Save it for when websocket is closed so that we can report it accurately
            this.error = e;
        }
    }

    @Override
    protected void hookOnComplete() {
        if (session.isOpen()) {
            sendRequests(COMPLETE);
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        if (session.isOpen()) {
            session.close(StatusCode.NORMAL, getError(throwable).toPrettyString(), Callback.NOOP);

            // Save it for when websocket is closed so that we can report it accurately
            this.error = throwable;
        }
    }

    @Override
    protected void hookOnCancel() {
        upstreamCancel();
        session.close(StatusCode.NORMAL, null, Callback.NOOP);
    }

    // Session.Listener.AutoDemanding
    @Override
    public void onWebSocketOpen(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteSocketAddress());
        }

        this.session = session;

        String serverContentType = session.getUpgradeResponse().getHeader(HttpHeader.CONTENT_TYPE.asString());
        String serverAcceptType = session.getUpgradeResponse().getHeader(HttpHeader.ACCEPT.asString());
        switch (subProtocol) {
            case publisher -> {
                if (!initReader(serverContentType))
                    return;
            }
            case subscriber -> {
                if (!initWriter(serverAcceptType))
                    return;
            }
            case subscriberWithResult -> {
                if (!initWriter(serverAcceptType))
                    return;
                if (!initReader(serverContentType))
                    return;
            }
        }

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
            session.sendText(contextJsonString, Callback.NOOP);
            logger.debug(marker, "Connected to {} with context {}", session.getRemoteSocketAddress(), contextJsonString);

            sink.onRequest(this::sendRequests);
        } catch (Throwable e) {
            error = e;
            session.close(StatusCode.NORMAL, getError(e).toPrettyString(), Callback.NOOP);
        }
    }

    private boolean initReader(String serverContentType) {
        if (serverContentType == null) {
            session.close(StatusCode.SERVER_ERROR, "publisher subprotocol requires Content-Type header", Callback.NOOP);
            return false;
        }
        reader = messageWorkers.newReader(inputType, inputType, serverContentType);
        if (reader == null) {
            session.close(StatusCode.SERVER_ERROR, "cannot handle Content-Type:" + serverContentType, Callback.NOOP);
            return false;
        }
        return true;
    }

    private boolean initWriter(String serverAcceptType) {
        if (serverAcceptType == null) {
            session.close(StatusCode.SERVER_ERROR, "publisher subprotocol requires Accept header", Callback.NOOP);
            return false;
        }
        writer = messageWorkers.newWriter(outputType, outputType, serverAcceptType);
        if (writer == null) {
            session.close(StatusCode.SERVER_ERROR, "cannot handle Accept type:" + serverAcceptType, Callback.NOOP);
            return false;
        }
        return true;
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
                if (requests == CANCEL) {
                    upstream().cancel();
                    session.close(StatusCode.NORMAL, null, Callback.NOOP);
                } else {
                    if (upstream() != null)
                        upstream().request(requests);
                    else
                        upstreamRequests.addAndGet(requests);
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
                contextMap.put("request", session.getUpgradeRequest());
                contextMap.put("response", session.getUpgradeResponse());
                context = reactor.util.context.Context.of(contextMap);
                publisher.subscribe(this);
            }
        } catch (Throwable e) {

            if (unwrap(e) instanceof EofException)
                return;

            upstream().cancel();
            sink.error(e);
            session.close(StatusCode.NORMAL, getError(e).toPrettyString(), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        if (reader == null) {
            if (!redundancyNotificationIssued) {
                logger.warn(marker, "Receiving redundant results from server");
                redundancyNotificationIssued = true;
            }
            callback.succeed();
        } else {
            try {
                if (logger.isTraceEnabled()) {
                    logger.trace(marker, "onWebSocketBinary {}", StandardCharsets.UTF_8.decode(payload.asReadOnlyBuffer()).toString());
                }
                INPUT event = reader.readFrom(new ByteBufferBackedInputStream(payload));
                outstandingRequests.decrementAndGet();
                sendRequests(SEND_BUFFERED_REQUESTS);
                callback.succeed();
                sink.next(event);
                receivedCounter.add(1, attributes);
            } catch (IOException e) {
                error = e;
                session.close(StatusCode.NORMAL, getError(e).toPrettyString(), Callback.NOOP);
                callback.fail(e);
            }
        }
    }

    @Override
    public void onWebSocketError(Throwable throwable) {

        Throwable unwrap = unwrap(throwable);
        if (unwrap instanceof ClosedChannelException
                || throwable instanceof EofException
                || throwable instanceof WebSocketTimeoutException)
            return;

        if (logger.isDebugEnabled()) {
            logger.debug(marker, "onWebSocketError", throwable);
        }
        if (error == null)
            error = throwable;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

/*
        // Upstream
        if (upstream() != null) {
            upstream().cancel();
        }
*/

        // Downstream
        if (statusCode == StatusCode.NORMAL) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            if (reason == null) {
                sink.complete();
            } else {
                try {
                    JsonNode reasonJson = jsonMapper.readTree(reason);
                    if (reasonJson instanceof ObjectNode objectNode) {
                        Throwable throwable;
                        String message = objectNode.path("reason").asText();

                        int status = reasonJson.get("status").asInt();
                        throwable = switch (status) {
                            case 401 -> new NotAuthorizedStreamException(message, error);

                            default -> new ServerStreamException(status, message, error);
                        };

                        sink.error(throwable);
                    } else {
                        sink.error(new ServerStreamException(statusCode, reason, error));
                    }
                } catch (JsonProcessingException e) {
                    sink.error(new ServerStreamException(statusCode, reason, error));
                }
            }
        } else {
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
            if (reason != null && reason.equals("Connection Idle Timeout")) {
                sink.error(new IdleTimeoutStreamException());
            } else {
                sink.error(new ServerStreamException(statusCode, reason, error));
            }
        }
        span.end();
    }

    private void upstreamCancel() {
        if (upstream() != null) {
            upstream().cancel();
        }
        session.close(StatusCode.NORMAL, null, Callback.NOOP);
    }

    private ObjectNode getError(Throwable throwable) {
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
    private void sendRequests(long n) {
        if (session == null || !session.isOpen())
            return;

        sendLock.lock();
        try {
            long rn = n;
            if (n == SEND_BUFFERED_REQUESTS) {
                rn = requests.get();
                if (rn >= sendRequestsThreshold && outstandingRequests.get() < sendRequestsThreshold) {
                    requests.addAndGet(-sendRequestsThreshold);
                    rn = sendRequestsThreshold;
                    outstandingRequests.addAndGet(rn);
                } else {
                    return;
                }
            } else if (n == COMPLETE) {
                if (requests.getAndSet(COMPLETE) == COMPLETE) {
                    // Already completed
                    return;
                }
            } else if (n != CANCEL) {
                rn = requests.addAndGet(n);

                if (sendRequestsThreshold == 0) {
                    sendRequestsThreshold = Math.max(1, (rn / 4) * 3);
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

            if (rn == 0) {
                return;
            }
            String requestString = Long.toString(rn);
            session.sendText(requestString, Callback.NOOP);

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}",
                        rn == CANCEL ? "CANCEL"
                                : rn == COMPLETE ? "COMPLETE"
                                : rn);
            else if (logger.isDebugEnabled())
                logger.debug(marker, "sendRequest {}",
                        rn == CANCEL ? "CANCEL"
                                : rn == COMPLETE ? "COMPLETE"
                                : rn);
        } finally {
            sendLock.unlock();
        }
    }
}
