package dev.xorcery.reactivestreams.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.xorcery.concurrent.SmartBatcher;
import dev.xorcery.dns.client.api.DnsLookup;
import dev.xorcery.io.ByteBufferBackedInputStream;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import dev.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerStreamException;
import dev.xorcery.reactivestreams.spi.MessageReader;
import dev.xorcery.reactivestreams.spi.MessageWorkers;
import dev.xorcery.reactivestreams.spi.MessageWriter;
import dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry;
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
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.*;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static dev.xorcery.lang.Exceptions.isCausedBy;
import static dev.xorcery.lang.Exceptions.unwrap;
import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class ClientSubscriberWithResultSubProtocolHandler<OUTPUT, INPUT>
        extends Session.Listener.AbstractAutoDemanding
        implements SubProtocolHandlerHelpers {

    private final static long CANCEL = Long.MIN_VALUE;
    private final static long COMPLETE = -1L;

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);


    private final Flux<OUTPUT> publisher;
    private final URI serverUri;
    private final ClientWebSocketOptions options;
    private final Class<OUTPUT> outputType;
    private final Class<INPUT> inputType;
    private final Collection<String> writeTypes;
    private final Collection<String> readTypes;
    private final MessageWorkers messageWorkers;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;
    private final ExecutorService flushingExecutors;
    private final String host;
    private final ByteBufferPool byteBufferPool;

    private final Logger logger;
    private final Marker marker;
    private final Meter meter;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final Attributes attributes;

    private final LongHistogram receivedBytes;
    private final LongHistogram itemReceivedSizes;
    private final LongHistogram sentBytes;
    private final LongHistogram itemSentSizes;
    private final LongHistogram requestsHistogram;
    private final LongHistogram flushHistogram;

    private Iterator<URI> uriIterator;
    private MessageWriter<OUTPUT> writer;
    private MessageReader<INPUT> reader;
    private int serverMaxBinaryMessageSize;
    private OutboundSubscriber outboundSubscriber;
    private String serverHost;
    private String clientHost;
    private final FluxSink<INPUT> inboundSink;
    private Throwable error;

    public ClientSubscriberWithResultSubProtocolHandler(
            Flux<OUTPUT> publisher,
            URI serverUri,
            FluxSink<INPUT> inboundSink,
            ClientWebSocketOptions options,
            Class<OUTPUT> outputType,
            Class<INPUT> inputType,
            Collection<String> writeTypes,
            Collection<String> readTypes,
            MessageWorkers messageWorkers,
            DnsLookup dnsLookup,
            WebSocketClient webSocketClient,
            ExecutorService flushingExecutors,
            String host,
            ByteBufferPool byteBufferPool,
            Meter meter,
            Tracer tracer,
            TextMapPropagator textMapPropagator,
            Logger logger) {
        this.publisher = publisher;
        this.serverUri = serverUri;
        this.inboundSink = inboundSink;
        this.inputType = inputType;
        this.options = options;
        this.outputType = outputType;
        this.writeTypes = writeTypes;
        this.readTypes = readTypes;
        this.messageWorkers = messageWorkers;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.flushingExecutors = flushingExecutors;
        this.host = host;
        this.byteBufferPool = byteBufferPool;
        this.meter = meter;
        this.tracer = tracer;
        this.textMapPropagator = textMapPropagator;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(serverUri.toASCIIString());

        this.attributes = Attributes.builder()
                .put(MessagingIncubatingAttributes.MESSAGING_SYSTEM, ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM)
                .put(UrlAttributes.URL_FULL, serverUri.toASCIIString())
                .put(ClientAttributes.CLIENT_ADDRESS, host)
                .build();
        this.receivedBytes = meter.histogramBuilder(SUBSCRIBER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.itemReceivedSizes = meter.histogramBuilder(SUBSCRIBER_ITEM_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.itemSentSizes = meter.histogramBuilder(PUBLISHER_ITEM_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();

        try {
            start();
        } catch (CompletionException e) {
            inboundSink.error(e.getCause());
        }
    }

    @Override
    public ReactiveStreamSubProtocol getSubProtocol() {
        return ReactiveStreamSubProtocol.subscriberWithResult;
    }

    @Override
    public MessageWorkers getMessageWorkers() {
        return messageWorkers;
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
                ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest(subscriberWebsocketUri);
                Map<String, List<String>> headers = options.headers();
                headers.put("Aggregate", List.of("maxBinaryMessageSize=" + webSocketClient.getMaxBinaryMessageSize()));
                clientUpgradeRequest.setHeaders(headers);
                clientUpgradeRequest.setCookies(options.cookies());
                clientUpgradeRequest.setExtensions(options.extensions().stream().map(ExtensionConfig::parse).toList());

                clientUpgradeRequest.setSubProtocols(ReactiveStreamSubProtocol.subscriberWithResult.name());
                clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), List.copyOf(writeTypes));
                clientUpgradeRequest.setHeader(HttpHeader.ACCEPT.asString(), List.copyOf(readTypes));
                textMapPropagator.inject(Context.current(), clientUpgradeRequest, jettySetter);
                return webSocketClient.connect(this, clientUpgradeRequest)
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

    @Override
    public void onWebSocketOpen(Session session) {
        super.onWebSocketOpen(session);

        serverHost = session.getRemoteSocketAddress().toString();
        clientHost = session.getLocalSocketAddress().toString();

        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", serverHost);
        }

        tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " connected " + serverUri.toASCIIString())
                .setSpanKind(SpanKind.CLIENT)
                .setAllAttributes(attributes)
                .startSpan()
                .setAttribute("server", serverHost)
                .setAttribute("client", clientHost)
                .end();

        String serverAcceptType = session.getUpgradeResponse().getHeader(HttpHeader.ACCEPT.asString());
        if ((writer = getWriter(serverAcceptType, outputType)) == null) {
            session.close(StatusCode.SHUTDOWN, "Cannot handle Accept type:" + serverAcceptType, Callback.NOOP);
            return;
        }
        String serverContentType = session.getUpgradeResponse().getHeader(HttpHeader.CONTENT_TYPE.asString());
        if ((reader = getReader(serverAcceptType, inputType)) == null) {
            session.close(StatusCode.SHUTDOWN, "Cannot handle Content-Type type:" + serverContentType, Callback.NOOP);
            return;
        }

        // Aggregate header for batching support
        serverMaxBinaryMessageSize = Optional.ofNullable(session.getUpgradeResponse().getHeader("Aggregate")).map(header ->
        {
            Map<String, String> parameters = new HashMap<>();
            Arrays.asList(header.split(";")).forEach(parameter -> {
                String[] paramKeyValue = parameter.split("=");
                parameters.put(paramKeyValue[0], paramKeyValue[1]);
            });
            return Integer.valueOf(parameters.computeIfAbsent("maxBinaryMessageSize", k -> "-1"));
        }).orElse(-1);

        // Send client subscriber context to the server subscriber
        ObjectNode clientSubscriberContext = createClientContext(inboundSink.contextView());

        try {
            String contextJsonString = jsonMapper.writeValueAsString(clientSubscriberContext);
            getSession().sendText(contextJsonString, Callback.NOOP);
            logger.debug(marker, "Sent context {}: {}", getSession().getRemoteSocketAddress(), contextJsonString);

        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        inboundSink.onDispose(() -> logger.debug("Dispose client stream"));
        inboundSink.onCancel(this::inboundCancel);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        try {
            JsonNode json = jsonMapper.readTree(message);
            if (json instanceof ObjectNode serverContextNode) {
                reactor.util.context.Context context = parseContext(serverContextNode);
                outboundSubscriber = new OutboundSubscriber();
                publisher.contextWrite(context).subscribe(outboundSubscriber);
                inboundSink.onRequest(this::sendRequest);
            } else if (json instanceof NumericNode numericNode) {
                if (outboundSubscriber != null) {
                    long requests = numericNode.asLong();
                    if (requests == CANCEL) {
                        outboundSubscriber.cancel();
                    } else if (requests == COMPLETE) {
                        // Cannot happen in this protocol
                        // Cannot receive context in this protocol
                        if (outboundSubscriber != null) {
                            outboundSubscriber.cancel();
                        }
                        getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
                    } else {
                        outboundSubscriber.request(numericNode.longValue());
                    }
                }
            } else if (json instanceof TextNode errorNode) {
                // Cannot happen in this protocol
                // Cannot receive errors in this protocol
                if (outboundSubscriber != null) {
                    outboundSubscriber.cancel();
                }
                getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
            }
        } catch (Throwable e) {

            if (unwrap(e) instanceof EofException)
                return;

            if (outboundSubscriber != null)
                outboundSubscriber.cancel();
            inboundSink.error(e);
            getSession().close(StatusCode.SHUTDOWN, e.getMessage(), Callback.NOOP);
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", StandardCharsets.UTF_8.decode(payload.asReadOnlyBuffer()).toString());
            }

            if (serverMaxBinaryMessageSize == -1) {
                receivedBytes.record(payload.limit(), attributes);
                itemReceivedSizes.record(payload.limit(), attributes);
                INPUT event = reader.readFrom(new ByteBufferBackedInputStream(payload));
                inboundSink.next(event);
            } else {
                receivedBytes.record(payload.limit(), attributes);
                while (payload.position() != payload.limit()) {
                    int length = payload.getInt();
                    ByteBuffer itemByteBuffer = payload.slice(payload.position(), length);
                    INPUT event = reader.readFrom(new ByteBufferBackedInputStream(itemByteBuffer));
                    payload.position(payload.position() + length);
                    inboundSink.next(event);
                    itemReceivedSizes.record(length, attributes);
                }
            }
            callback.succeed();
        } catch (Throwable e) {
            logger.error("Could not receive value", e);
            inboundSink.error(e);
            if (outboundSubscriber != null) {
                outboundSubscriber.cancel();
            }
            callback.fail(e);
        }
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        error = throwable;

        Throwable unwrap = unwrap(throwable);
        if (unwrap instanceof ClosedChannelException
                || throwable instanceof EofException
                || throwable instanceof WebSocketTimeoutException)
            return;

        if (logger.isDebugEnabled()) {
            logger.debug(marker, "onWebSocketError", throwable);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        // Cancel publisher
        if (outboundSubscriber != null) {
            outboundSubscriber.cancel();
        }

        switch (statusCode) {
            case StatusCode.NORMAL -> {
                switch (reason) {
                    case "complete" -> inboundSink.complete();
                    default -> inboundSink.error(error != null
                            ? new ServerStreamException(statusCode, reason, error)
                            : new ServerStreamException(statusCode, reason));
                }
            }
            default -> {
                switch (reason) {
                    case IdleTimeoutStreamException.CONNECTION_IDLE_TIMEOUT ->
                            inboundSink.error(new IdleTimeoutStreamException());
                    default -> inboundSink.error(error != null
                            ? new ServerStreamException(statusCode, reason, error)
                            : new ServerStreamException(statusCode, reason));
                }
            }
        }

        tracer.spanBuilder("stream " + getSubProtocol() + " disconnected " + serverUri.toASCIIString())
                .setSpanKind(SpanKind.CLIENT)
                .setAllAttributes(attributes)
                .startSpan()
                .setAttribute("reason", reason)
                .setAttribute("statusCode", statusCode)
                .setAttribute("server", serverHost)
                .setAttribute("client", clientHost)
                .end();
    }

    private void inboundCancel() {
        sendRequest(CANCEL);
    }

    // Send requests
    protected void sendRequest(long n) {
        Session session = getSession();
        if (session == null || !session.isOpen())
            return;

        // Send the request over the network
        getSession().sendText(Long.toString(n), Callback.NOOP);
        requestsHistogram.record(n);
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "sendRequest {}", n);
        }
    }

    private class OutboundSubscriber
            extends BaseSubscriber<OUTPUT> {

        private final SmartBatcher<OUTPUT> batcher;

        private final int queueSize = 1024; // TODO Make configurable
        private final int[] sizes = new int[queueSize];

        public OutboundSubscriber() {
            this.batcher = new SmartBatcher<>(this::flush, new ArrayBlockingQueue<>(queueSize), flushingExecutors);
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
        }

        @Override
        protected void hookOnNext(OUTPUT item) {

            if (serverMaxBinaryMessageSize == -1) {
                try (ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(byteBufferPool, false)) {
                    writer.writeTo(item, outputStream);

                    RetainableByteBuffer retainableByteBuffer = outputStream.takeByteBuffer();
                    ByteBuffer eventBuffer = retainableByteBuffer.getByteBuffer();
                    sentBytes.record(eventBuffer.limit(), attributes);
                    itemSentSizes.record(eventBuffer.limit(), attributes);
                    getSession().sendBinary(eventBuffer, new ReleaseCallback(retainableByteBuffer));
                } catch (Throwable e) {

                    if (isCausedBy(e, EofException.class, ClosedChannelException.class)) {
                        // Shutting down, ignore this
                        return;
                    }

                    // Tell server we have a problem
                    onError(e);
                }
            } else {
                try {
                    batcher.submit(item);
                } catch (InterruptedException e) {
                    onError(e);
                }
            }
        }

        private void flush(Collection<OUTPUT> items) {
            RetainableByteBuffer sendByteBuffer = byteBufferPool.acquire(serverMaxBinaryMessageSize, false);
            ByteBuffer byteBuffer = sendByteBuffer.getByteBuffer();
            logger.debug("Flushing {}", items.size());
            try (ByteBufferOutputStream outputStream = new ByteBufferOutputStream(byteBuffer)) {
                int idx = 0;
                for (OUTPUT item : items) {
                    outputStream.write(new byte[4]);
                    int position = byteBuffer.limit();
                    boolean isBufferOverflow = false;
                    try {
                        writer.writeTo(item, outputStream);
                    } catch (Throwable e) {
                        if (unwrap(e) instanceof BufferOverflowException) {
                            isBufferOverflow = true;
                        } else {
                            throw e;
                        }
                    }

                    if (byteBuffer.limit() > serverMaxBinaryMessageSize || isBufferOverflow) {
                        logger.info("OVERFLOW FLUSH {}", byteBuffer.limit());

                        // Flush what we have so far and start again
                        position = 0;
                        for (int i = 0; i < idx; i++) {
                            int size = sizes[i];
                            byteBuffer.putInt(size);
                            position += 4 + size;
                            byteBuffer.position(position);
                            itemSentSizes.record(size, attributes);
                        }
                        byteBuffer.flip();
                        CompletableFuture<Void> flushDone = new CompletableFuture<>();
                        getSession().sendBinary(byteBuffer, Callback.from(() -> flushDone.complete(null), flushDone::completeExceptionally));
                        flushDone.join();
                        byteBuffer.limit(0);
                        idx = 0;

                        // Try again
                        outputStream.write(new byte[4]);
                        position = byteBuffer.limit();
                        writer.writeTo(item, outputStream);

                        if (byteBuffer.limit() > serverMaxBinaryMessageSize) {
                            throw new IOException(String.format("Item is too large:%d bytes, server max binary message size:%d", byteBuffer.limit(), serverMaxBinaryMessageSize));
                        }
                    }

                    int size = byteBuffer.limit() - position;
                    sizes[idx++] = size;
                }

                // Fill in the item sizes
                int position = 0;
                for (int i = 0; i < idx; i++) {
                    int size = sizes[i];
                    byteBuffer.putInt(size);
                    position += 4 + size;
                    byteBuffer.position(position);
                    itemSentSizes.record(size, attributes);
                }
                byteBuffer.flip();

                sentBytes.record(byteBuffer.limit(), attributes);
                flushHistogram.record(items.size(), attributes);
                getSession().sendBinary(byteBuffer, new ReleaseCallback(sendByteBuffer));
            } catch (Throwable e) {
                logger.error("Flush failed", e);
                onError(e);
            }
        }

        @Override
        protected void hookOnComplete() {
            if (getSession().isOpen()) {
                if (batcher != null) {
                    batcher.close();
                }
                logger.debug("Complete");
                sendRequest(COMPLETE);
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {

            if (getSession().isOpen()) {
                if (batcher != null) {
                    batcher.close();
                }
                logger.debug("Error", throwable);
                getSession().sendText(JsonNodeFactory.instance.textNode(throwable.getMessage()).toString(), Callback.NOOP);
            }
        }

        @Override
        protected void hookOnCancel() {
        }
    }
}
