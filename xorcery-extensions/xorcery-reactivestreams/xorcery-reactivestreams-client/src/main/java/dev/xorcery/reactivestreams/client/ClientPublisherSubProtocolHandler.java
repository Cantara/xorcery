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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.dns.client.api.DnsLookup;
import dev.xorcery.io.ByteBufferBackedInputStream;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import dev.xorcery.reactivestreams.api.IdleTimeoutStreamException;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerStreamException;
import dev.xorcery.reactivestreams.spi.MessageReader;
import dev.xorcery.reactivestreams.spi.MessageWorkers;
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
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import reactor.core.publisher.FluxSink;

import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;

import static dev.xorcery.lang.Exceptions.unwrap;
import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class ClientPublisherSubProtocolHandler<INPUT>
        extends Session.Listener.AbstractAutoDemanding
        implements SubProtocolHandlerHelpers {
    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final URI serverUri;
    private final FluxSink<INPUT> inboundSink;
    private final ClientWebSocketOptions options;
    private final Class<? super INPUT> inputType;
    private final Collection<String> readTypes;
    private final MessageWorkers messageWorkers;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;
    private final ExecutorService flushingExecutors;
    private final String host;
    private final ByteBufferPool byteBufferPool;

    private final Logger logger;
    private final Marker marker;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final Attributes attributes;

    private final LongHistogram receivedBytes;
    private final LongHistogram itemReceivedSizes;
    private final LongHistogram requestsHistogram;

    private Iterator<URI> uriIterator;
    private MessageReader<INPUT> reader;
    private int serverMaxBinaryMessageSize;
    private String serverHost;
    private String clientHost;
    private Throwable error;

    public  ClientPublisherSubProtocolHandler(
            URI serverUri,
            FluxSink<INPUT> inboundSink,
            ClientWebSocketOptions options,
            Class<? super INPUT> inputType,
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
        this.serverUri = serverUri;
        this.inboundSink = inboundSink;
        this.options = options;
        this.inputType = inputType;
        this.readTypes = readTypes;
        this.messageWorkers = messageWorkers;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.flushingExecutors = flushingExecutors;
        this.host = host;
        this.byteBufferPool = byteBufferPool;
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
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();

        try {
            start();
        } catch (CompletionException e) {
            inboundSink.error(e.getCause());
        }
    }

    @Override
    public ReactiveStreamSubProtocol getSubProtocol() {
        return ReactiveStreamSubProtocol.publisher;
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

                clientUpgradeRequest.setSubProtocols(ReactiveStreamSubProtocol.publisher.name());
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
            logger.trace(marker, "onWebSocketOpen {}", serverHost);
        }

        tracer.spanBuilder("stream " + ReactiveStreamSubProtocol.publisher + " connected " + serverUri.toASCIIString())
                .setSpanKind(SpanKind.CLIENT)
                .setAllAttributes(attributes)
                .startSpan()
                .setAttribute("server", serverHost)
                .setAttribute("client", clientHost)
                .end();

        String serverContentType = session.getUpgradeResponse().getHeader(HttpHeader.CONTENT_TYPE.asString());
        if ((reader = getReader(serverContentType, inputType)) == null) {
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

        // Send client subscriber context to the server publisher
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
        inboundSink.onRequest(this::sendRequest);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }
        getSession().close(StatusCode.PROTOCOL, "wrongProtocol", Callback.NOOP);
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
            callback.fail(e);
            getSession().close(StatusCode.NORMAL, "cancel", Callback.NOOP);
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
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
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
                        case IdleTimeoutStreamException.CONNECTION_IDLE_TIMEOUT,
                             IdleTimeoutStreamException.SESSION_IDLE_TIMEOUT ->
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
        } finally {
            callback.succeed();
        }
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
}
