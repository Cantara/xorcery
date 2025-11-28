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
package dev.xorcery.reactivestreams.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.xorcery.concurrent.SmartBatcher;
import dev.xorcery.lang.Exceptions;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.*;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.Disposable;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static dev.xorcery.lang.Exceptions.isCausedBy;
import static dev.xorcery.lang.Exceptions.unwrap;
import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;
import static io.opentelemetry.api.trace.StatusCode.ERROR;
import static io.opentelemetry.api.trace.StatusCode.OK;

public class ServerPublisherSubProtocolHandler<OUTPUT>
        extends Session.Listener.AbstractAutoDemanding
        implements Disposable, SubProtocolHandlerHelpers {
    private final AtomicLong connectionCounter;
    private final ServerWebSocketOptions options;
    private final MessageWriter<OUTPUT> writer;
    private final Publisher<OUTPUT> publisher;
    private final String path;
    private final Map<String, String> pathParameters;
    private final int clientMaxBinaryMessageSize;
    private final ExecutorService flushingExecutors;
    private final ByteBufferPool byteBufferPool;
    private final Logger logger;
    private final Tracer tracer;
    private final Context requestContext;
    private final Attributes attributes;

    private final LongHistogram sentBytes;
    private final LongHistogram itemSentSizes;
    private final LongHistogram flushHistogram;
    private final LongHistogram requestsHistogram;
    private final Marker marker;

    private Map<String, List<String>> parameterMap;
    private OutboundSubscriber outboundSubscriber;
    private String serverHost;
    private String clientHost;

    private Throwable error;

    public ServerPublisherSubProtocolHandler(
            AtomicLong connectionCounter,

            ServerWebSocketOptions options,
            MessageWriter<OUTPUT> writer,
            Publisher<OUTPUT> publisher,

            String path,
            Map<String, String> pathParameters,
            int clientMaxBinaryMessageSize,

            ExecutorService flushingExecutors,
            ByteBufferPool byteBufferPool,

            Logger logger,
            Tracer tracer,
            Meter meter,
            Context requestContext,
            Attributes attributes
    ) {
        this.connectionCounter = connectionCounter;
        this.options = options;
        this.writer = writer;
        this.publisher = publisher;
        this.path = path;
        this.pathParameters = pathParameters;
        this.clientMaxBinaryMessageSize = clientMaxBinaryMessageSize;
        this.flushingExecutors = flushingExecutors;
        this.byteBufferPool = byteBufferPool;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(path);
        this.tracer = tracer;
        this.requestContext = requestContext;
        this.attributes = attributes;

        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.itemSentSizes = meter.histogramBuilder(PUBLISHER_ITEM_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
    }

    @Override
    public void onWebSocketOpen(Session session) {
        super.onWebSocketOpen(session);

        serverHost = session.getRemoteSocketAddress().toString();
        clientHost = session.getLocalSocketAddress().toString();

        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketOpen {}", serverHost);

        this.parameterMap = session.getUpgradeRequest().getParameterMap();
        session.setMaxOutgoingFrames(options.maxOutgoingFrames());
        connectionCounter.incrementAndGet();

        tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " connected " + path)
                .setParent(requestContext)
                .setSpanKind(SpanKind.SERVER)
                .setAllAttributes(attributes)
                .startSpan()
                .setAttribute("remote", serverHost)
                .setAttribute("local", clientHost)
                .end();
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        try {
            JsonNode json = jsonMapper.readTree(message);
            if (json instanceof ObjectNode clientContext) {
                // Context from client subscriber
                clientSubscribe(clientContext);
            } else if (json instanceof NumericNode numericNode) {
                if (numericNode.asLong() == CANCEL && outboundSubscriber != null) {
                    outboundSubscriber.cancel();
                } else {
                    long n = numericNode.longValue();
                    if (n > 0)
                        requestsHistogram.record(n);
                    outboundSubscriber.request(n);
                }
            } else if (json instanceof TextNode) {
                getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
            } else {
                logger.error("Unknown JSON type:" + json + "(" + message + ")");
                getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
            }
        } catch (Throwable e) {
            logger.error("onWebSocketText error", e);
            getSession().close(StatusCode.NORMAL, e.getMessage(), Callback.NOOP);
        }
    }

    private void clientSubscribe(ObjectNode clientContext) throws JsonProcessingException {
        reactor.util.context.Context context = parseContext(clientContext, pathParameters, parameterMap);

        // Subscribe upstream
        Flux<OUTPUT> outboundResultFlux = switch (publisher) {
            case Flux<OUTPUT> flux -> flux;
            case Publisher<OUTPUT> publisher -> Flux.from(publisher);
        };

        outboundSubscriber = new OutboundSubscriber();
        outboundResultFlux
                .contextWrite(context)
                .subscribe(outboundSubscriber);
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketBinary {}", StandardCharsets.UTF_8.decode(payload.asReadOnlyBuffer()).toString());
        }

        getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
        callback.succeed();
    }

    @Override
    public void onWebSocketError(Throwable throwable) {
        if (logger.isDebugEnabled() && !Exceptions.isCausedBy(throwable, ClosedChannelException.class)) {
            logger.debug(marker, "onWebSocketError", throwable);
        }
        error = throwable;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
            } else {
                logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            }

            switch (statusCode) {
                case StatusCode.NORMAL -> {
                    switch (reason) {
                        case "complete" -> {
                        }
                        case "cancel" -> outboundSubscriber.cancel();
                    }
                }
                default-> outboundSubscriber.cancel();
            }
        } finally {
            tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " disconnected " + path)
                    .setParent(requestContext)
                    .setSpanKind(SpanKind.SERVER)
                    .setAllAttributes(attributes)
                    .startSpan()
                    .setStatus(statusCode==StatusCode.NORMAL ? OK : ERROR, error != null ? error.getMessage() : null)
                    .setAttribute("reason", reason)
                    .setAttribute("statusCode", statusCode)
                    .setAttribute("client", clientHost)
                    .setAttribute("server", serverHost)
                    .end();
            connectionCounter.decrementAndGet();
            callback.succeed();
        }
    }

    @Override
    public void dispose() {
        if (getSession().isOpen()){
            getSession().close(StatusCode.SHUTDOWN, "dispose", Callback.NOOP);
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

            if (clientMaxBinaryMessageSize == -1) {
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
            RetainableByteBuffer sendByteBuffer = byteBufferPool.acquire(clientMaxBinaryMessageSize, false);
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

                    if (byteBuffer.limit() > clientMaxBinaryMessageSize || isBufferOverflow) {
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

                        if (byteBuffer.limit() > clientMaxBinaryMessageSize) {
                            throw new IOException(String.format("Item is too large:%d bytes, client max binary message size:%d", byteBuffer.limit(), clientMaxBinaryMessageSize));
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
                getSession().close(StatusCode.NORMAL, "complete", Callback.NOOP);
            }
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            if (getSession().isOpen()) {
                if (batcher != null) {
                    batcher.close();
                }
                logger.error("Upstream error", throwable);
                getSession().close(StatusCode.NORMAL, throwable.getMessage(), Callback.NOOP);
            }
        }

        @Override
        protected void hookOnCancel() {
        }
    }
}
