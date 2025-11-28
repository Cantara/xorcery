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
package dev.xorcery.reactivestreams.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.xorcery.concurrent.SmartBatcher;
import dev.xorcery.dns.client.api.DnsLookup;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import dev.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.api.client.ClientStreamException;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
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

import java.io.IOException;
import java.net.URI;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.*;
import java.util.concurrent.*;

import static dev.xorcery.lang.Exceptions.isCausedBy;
import static dev.xorcery.lang.Exceptions.unwrap;
import static dev.xorcery.reactivestreams.api.IdleTimeoutStreamException.CONNECTION_IDLE_TIMEOUT;
import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.api.trace.StatusCode.OK;

public class ClientSubscriberSubProtocolHandler<OUTPUT>
        extends Session.Listener.AbstractAutoDemanding
        implements SubProtocolHandlerHelpers {

    private final static long CANCEL = Long.MIN_VALUE;
    private final static long COMPLETE = -1L;

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final Flux<OUTPUT> publisher;
    private final URI serverUri;
    private final CompletableFuture<Void> result;
    private final ClientWebSocketOptions options;
    private final Class<? super OUTPUT> outputType;
    private final Collection<String> writeTypes;
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
    private final LongHistogram sentBytes;
    private final LongHistogram itemSentSizes;
    private final LongHistogram requestsHistogram;
    private final LongHistogram flushHistogram;

    private Iterator<URI> uriIterator;
    private MessageWriter<OUTPUT> writer;
    private int serverMaxBinaryMessageSize;
    private OutboundSubscriber outboundSubscriber;
    private String serverHost;
    private String clientHost;

    private Throwable error;

    public ClientSubscriberSubProtocolHandler(
            Flux<OUTPUT> publisher,
            URI serverUri,
            CompletableFuture<Void> result,
            ClientWebSocketOptions options,
            Class<? super OUTPUT> outputType,
            Collection<String> writeTypes,
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
        this.result = result;
        this.options = options;
        this.outputType = outputType;
        this.writeTypes = writeTypes;
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
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.itemSentSizes = meter.histogramBuilder(PUBLISHER_ITEM_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();

        result.exceptionally(throwable -> {
            if (throwable instanceof CancellationException) {
                getSession().sendText(JsonNodeFactory.instance.numberNode(CANCEL).asText(), Callback.NOOP);
            }
            return null;
        });

        try {
            start();
        } catch (CompletionException e) {
            result.completeExceptionally(e.getCause());
        }
    }

    @Override
    public ReactiveStreamSubProtocol getSubProtocol() {
        return ReactiveStreamSubProtocol.subscriber;
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

                clientUpgradeRequest.setSubProtocols(ReactiveStreamSubProtocol.subscriber.name());
                clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), List.copyOf(writeTypes));
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
            logger.trace(marker, "onWebSocketOpen {}", serverHost);
        }

        tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " connected " + serverUri.toASCIIString())
                .setSpanKind(SpanKind.CLIENT)
                .setAllAttributes(attributes)
                .startSpan()
                .setAttribute("server", serverHost)
                .setAttribute("client", clientHost)
                .end();

        String serverAcceptType = session.getUpgradeResponse().getHeader(HttpHeader.ACCEPT.asString());
        if ((writer = getWriter(serverAcceptType, outputType)) == null){
            session.close(StatusCode.SHUTDOWN, "Cannot handle Accept type:"+serverAcceptType, Callback.NOOP);
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
            } else if (json instanceof NumericNode numericNode) {
                if (outboundSubscriber != null) {
                    long requests = numericNode.asLong();
                    if (requests == CANCEL) {
                        outboundSubscriber.cancel();
                        getSession().close(StatusCode.NORMAL, "cancel", Callback.NOOP);
                    } else if (requests == COMPLETE) {
                        // Cannot happen in this protocol
                        // Cannot receive context in this protocol
                        if (outboundSubscriber != null) {
                            outboundSubscriber.cancel();
                        }
                        getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
                    } else {
                        long n = numericNode.longValue();
                        if (n > 0)
                            requestsHistogram.record(n);
                        outboundSubscriber.request(n);
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

            outboundSubscriber.cancel();
            getSession().close(StatusCode.SHUTDOWN, e.getMessage(), Callback.NOOP);
        }
    }


    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        // Cannot happen in this protocol
        // Cannot receive items in this protocol
        if (outboundSubscriber != null) {
            outboundSubscriber.cancel();
        }
        getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        result.completeExceptionally(throwable);

        Throwable unwrap = unwrap(throwable);
        if (unwrap instanceof ClosedChannelException
                || throwable instanceof EofException
                || throwable instanceof WebSocketTimeoutException)
            return;

        if (logger.isDebugEnabled()) {
            logger.debug(marker, "onWebSocketError", throwable);
        }

        error = throwable;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
            }

            // Upstream
            if (outboundSubscriber != null) {
                outboundSubscriber.cancel();
            }

            if (!result.isDone()) {
                if (reason.equals("complete")) {
                    result.complete(null);
                } else if (reason.equals("cancel")) {
                    result.cancel(true);
                } else if (reason.equals(CONNECTION_IDLE_TIMEOUT)) {
                    result.completeExceptionally(new IdleTimeoutStreamException());
                } else {
                    result.completeExceptionally(new ClientStreamException(statusCode, reason));
                }
            }

            tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " disconnected " + serverUri.toASCIIString())
                    .setSpanKind(SpanKind.CLIENT)
                    .setAllAttributes(attributes)
                    .startSpan()
                    .setStatus(statusCode==StatusCode.NORMAL ? OK : ERROR, error != null ? error.getMessage() : null)
                    .setAttribute("reason", reason)
                    .setAttribute("statusCode", statusCode)
                    .setAttribute("server", serverHost)
                    .setAttribute("client", clientHost)
                    .end();
        } finally {
            callback.succeed();
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
                    result.completeExceptionally(e);
                }
            } else {
                try {
                    batcher.submit(item);
                } catch (InterruptedException e) {
                    onError(e);
                    result.completeExceptionally(e);
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
                result.completeExceptionally(e);
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
                getSession().close(StatusCode.NORMAL, "complete", Callback.NOOP);
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {

            result.completeExceptionally(new ClientStreamException(StatusCode.NORMAL, throwable.getMessage(), throwable));

            if (getSession().isOpen()) {
                if (batcher != null) {
                    batcher.close();
                }
                logger.debug("Error", throwable);
                getSession().close(StatusCode.NORMAL, throwable.getMessage(), Callback.NOOP);
            }
        }

        @Override
        protected void hookOnCancel() {
        }
    }
}
