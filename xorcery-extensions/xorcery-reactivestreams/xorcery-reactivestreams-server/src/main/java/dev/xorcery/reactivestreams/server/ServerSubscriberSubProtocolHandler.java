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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.xorcery.io.ByteBufferBackedInputStream;
import dev.xorcery.lang.Exceptions;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import dev.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.api.client.ClientShutdownStreamException;
import dev.xorcery.reactivestreams.api.client.ClientStreamException;
import dev.xorcery.reactivestreams.api.server.ServerStreamException;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.spi.MessageReader;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static dev.xorcery.reactivestreams.api.IdleTimeoutStreamException.CONNECTION_IDLE_TIMEOUT;
import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class ServerSubscriberSubProtocolHandler<INPUT>
        extends Session.Listener.AbstractAutoDemanding
        implements Disposable {
    private final static ObjectMapper jsonMapper = new JsonMapper().findAndRegisterModules();

    private final static long CANCEL = Long.MIN_VALUE;

    private final MessageReader<INPUT> reader;
    private final Function<Flux<INPUT>, Disposable> subscriber;
    private final String path;
    private final Map<String, String> pathParameters;
    private final ServerWebSocketOptions options;
    private final int clientMaxBinaryMessageSize;
    private final Logger logger;
    private final Marker marker;
    private final Tracer tracer;
    private final Context requestContext;
    private final Attributes attributes;

    private Map<String, List<String>> parameterMap;
    private final AtomicLong connectionCounter;
    private FluxSink<INPUT> inboundSink;
    private Disposable subscriptionDisposable;

    private final LongHistogram receivedBytes;
    private final LongHistogram itemReceivedSizes;
    private final LongHistogram requestsHistogram;

    public ServerSubscriberSubProtocolHandler(
            AtomicLong connectionCounter,

            ServerWebSocketOptions options,
            MessageReader<INPUT> reader,
            Function<Flux<INPUT>, Disposable> subscriber,

            String path,
            Map<String, String> pathParameters,
            int clientMaxBinaryMessageSize,

            Logger logger,
            Tracer tracer,
            Meter meter,
            io.opentelemetry.context.Context requestContext,
            Attributes attributes
    ) {
        this.reader = reader;
        this.subscriber = subscriber;
        this.path = path;
        this.pathParameters = pathParameters;
        this.options = options;
        this.connectionCounter = connectionCounter;
        this.clientMaxBinaryMessageSize = clientMaxBinaryMessageSize;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(path);
        this.tracer = tracer;
        this.requestContext = requestContext;
        this.attributes = attributes;

        this.receivedBytes = meter.histogramBuilder(SUBSCRIBER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.itemReceivedSizes = meter.histogramBuilder(SUBSCRIBER_ITEM_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
    }

    @Override
    public void onWebSocketOpen(Session session) {
        super.onWebSocketOpen(session);

        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketOpen {}", session.getRemoteSocketAddress());

        this.parameterMap = session.getUpgradeRequest().getParameterMap();
        session.setMaxOutgoingFrames(options.maxOutgoingFrames());
        connectionCounter.incrementAndGet();

        tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " connected " + path)
                .setParent(requestContext)
                .setSpanKind(SpanKind.SERVER)
                .setAllAttributes(attributes)
                .startSpan()
                .setAttribute("remote", session.getRemoteSocketAddress().toString())
                .setAttribute("local", session.getLocalSocketAddress().toString())
                .end();

        // Subscribe upstream
        Flux<INPUT> inboundFlux = Flux.create(inboundSink -> {
            this.inboundSink = inboundSink;

            // Send server subscriber context back to client
            ObjectNode serverContext = JsonNodeFactory.instance.objectNode();
            inboundSink.contextView().forEach((k, v) ->
            {
                if (v instanceof String str) {
                    serverContext.set(k.toString(), serverContext.textNode(str));
                } else if (v instanceof Long nr) {
                    serverContext.set(k.toString(), serverContext.numberNode(nr));
                } else if (v instanceof Double nr) {
                    serverContext.set(k.toString(), serverContext.numberNode(nr));
                } else if (v instanceof Boolean bool) {
                    serverContext.set(k.toString(), serverContext.booleanNode(bool));
                }
            });

            pathParameters.forEach((k, v) -> serverContext.set(k, JsonNodeFactory.instance.textNode(v)));
            parameterMap.forEach((k, v) -> serverContext.set(k, JsonNodeFactory.instance.textNode(v.get(0))));

            try {
                String contextJsonString = jsonMapper.writeValueAsString(serverContext);
                getSession().sendText(contextJsonString, Callback.NOOP);
                logger.debug(marker, "Sent context {}: {}", getSession().getRemoteSocketAddress(), contextJsonString);

            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }

            inboundSink.onDispose(() -> logger.debug("Server stream terminated"));
            inboundSink.onCancel(this::inboundCancel);
            inboundSink.onRequest(this::sendRequest);
        });

        subscriptionDisposable = subscriber.apply(inboundFlux);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        try {
            JsonNode json = jsonMapper.readTree(message);
            if (json instanceof ObjectNode) {
                // Cannot receive context in this protocol
                if (subscriptionDisposable != null) {
                    subscriptionDisposable.dispose();
                } else {
                    getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
                }
            } else if (json instanceof NumericNode numericNode) {
                if (numericNode.asLong() == CANCEL && subscriptionDisposable != null) {
                    subscriptionDisposable.dispose();
                } else {
                    // Cannot receive requests in this protocol
                    getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
                }
            } else if (json instanceof TextNode errorNode) {
                inboundSink.error(new ClientStreamException(1001, errorNode.asText()));
            } else {
                logger.error("Unknown JSON type:" + json + "(" + message + ")");
            }
        } catch (Throwable e) {
            logger.error("onWebSocketText error", e);
            if (inboundSink != null)
                inboundSink.error(e);
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", StandardCharsets.UTF_8.decode(payload.asReadOnlyBuffer()).toString());
            }

            if (clientMaxBinaryMessageSize == -1) {
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
            if (inboundSink != null)
                inboundSink.error(e);
            callback.fail(e);
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
            } else {
                logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            }

            if ("cancel".equals(reason))
                return;

            switch (statusCode) {
                case StatusCode.NORMAL-> {
                    switch (reason){
                        case "complete" -> inboundSink.complete();
                        default -> inboundSink.error(new ServerStreamException(statusCode, reason));
                    }
                }
                case StatusCode.SHUTDOWN-> {
                    if (CONNECTION_IDLE_TIMEOUT.equals(reason)) {
                        inboundSink.error(new IdleTimeoutStreamException());
                    } else {
                        inboundSink.error(new ClientShutdownStreamException(reason));
                    }
                }
                default-> inboundSink.error(new ClientStreamException(statusCode, reason));
            }
        } finally {
            tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.subscriber + " disconnected " + path)
                    .setParent(requestContext)
                    .setSpanKind(SpanKind.SERVER)
                    .setAllAttributes(attributes)
                    .startSpan()
                    .setAttribute("reason", reason)
                    .setAttribute("statusCode", statusCode)
                    .setAttribute("client", getSession().getRemoteSocketAddress().toString())
                    .setAttribute("server", getSession().getLocalSocketAddress().toString())
                    .end();
            connectionCounter.decrementAndGet();
        }
    }

    @Override
    public void dispose() {
        if (subscriptionDisposable != null) {
            subscriptionDisposable.dispose();
        }
    }

    private void inboundCancel() {
        getSession().close(StatusCode.NORMAL, "cancel", Callback.NOOP);
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
}
