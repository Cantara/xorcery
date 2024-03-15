package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.reactivestreams.api.client.ClientBadPayloadStreamException;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerTimeoutStreamException;
import com.exoreaction.xorcery.reactivestreams.client.WriteCallbackCompletableFuture;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class SubscribeWebSocketStream
        implements
        WebSocketListener,
        Subscription {

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final static JsonMapper jsonMapper = new JsonMapper();

    private final WebSocketClient webSocketClient;
    private final CoreSubscriber<Object> subscriber;
    private final WebSocketClientOptions options;
    private final MessageReader<Object> eventReader;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final Logger logger;
    private final Iterator<URI> uriIterator;

    private volatile Marker marker;
    protected volatile Session session;
    private volatile Throwable webSocketError;

    private final String mediaType;
    private final AtomicBoolean isDisposed;
    private final AtomicBoolean isCancelled = new AtomicBoolean(); // true if cancel() has been called or the result has been completed
    protected final AtomicBoolean isComplete = new AtomicBoolean(); // true if onComplete or onError has been called

    private volatile long sendRequestsThreshold = 0;
    private final AtomicLong requests = new AtomicLong();
    private final AtomicLong outstandingRequests = new AtomicLong();

    private final Lock sendLock = new ReentrantLock();
    private final SendWriteCallback sendWriteCallback = new SendWriteCallback();

    public SubscribeWebSocketStream(
            Iterator<URI> uriIterator,
            String mediaType,
            CoreSubscriber<Object> subscriber,
            AtomicBoolean isDisposed,
            WebSocketClient webSocketClient,
            WebSocketClientOptions options,
            MessageReader<Object> eventReader,
            Tracer tracer,
            final TextMapPropagator textMapPropagator,
            Logger logger
    ) {
        this.uriIterator = uriIterator;
        this.mediaType = mediaType;
        this.isDisposed = isDisposed;
        this.subscriber = subscriber;

        this.webSocketClient = webSocketClient;
        this.options = options;
        this.eventReader = eventReader;
        this.tracer = tracer;
        this.textMapPropagator = textMapPropagator;
        this.logger = logger;

        this.marker = MarkerManager.getMarker("reactorstream");

        connect();
    }

    // Connection process
    public void connect() {
        if (!webSocketClient.isStarted()) {
            subscriber.onError(new IllegalStateException("WebSocketClient not started"));
            isDisposed.set(true);
            return;
        }

        while (uriIterator.hasNext()) {
            URI publisherWebsocketUri = uriIterator.next();
            this.marker = MarkerManager.getMarker(publisherWebsocketUri.toASCIIString());

            if (logger.isTraceEnabled()) {
                logger.trace(marker, "connect {}", publisherWebsocketUri);
            }

            reactor.util.context.Context context = subscriber.currentContext();
            List<String> params = context.stream().map(entry -> entry.getKey().toString() + "=" + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8)).toList();
            String queryParameters = "?" + String.join("&", params);
            URI effectivePublisherWebsocketUri = URI.create(publisherWebsocketUri.toASCIIString() + queryParameters);
            logger.debug(marker, "Trying " + effectivePublisherWebsocketUri);
            Span connectSpan = tracer.spanBuilder(publisherWebsocketUri.toASCIIString() + " connect")
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();
            try (Scope scope = connectSpan.makeCurrent()) {
                ClientUpgradeRequest clientUpgradeRequest = new ClientUpgradeRequest();
                clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), mediaType);
                clientUpgradeRequest.setCookies(options.cookies());
                clientUpgradeRequest.setHeaders(options.headers());
                clientUpgradeRequest.setSubProtocols(options.subProtocols());
                clientUpgradeRequest.setExtensions(options.extensions().stream().map(ExtensionConfig::parse).toList());

                textMapPropagator.inject(Context.current(), clientUpgradeRequest, jettySetter);
                webSocketClient.connect(this, effectivePublisherWebsocketUri, clientUpgradeRequest)
                        .thenAccept(this::connected)
                        .exceptionally(this::connectException)
                        .thenRun(connectSpan::end);
                return;
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + effectivePublisherWebsocketUri.toASCIIString(), e);
            }
        }
    }

    private void connected(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "connected");
        }
        this.session = session;
    }

    private Void connectException(Throwable throwable) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "exceptionally", throwable);
        }
        subscriber.onError(throwable);
        return null;
    }

    // Subscription
    @Override
    public void request(long n) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "request {}", n);
        }

        sendRequests(n);
    }

    @Override
    public void cancel() {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "cancel");
        }

        isCancelled.set(true);
        sendRequests(0);
    }

    // WebSocketListener
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }
        this.session = session;

        logger.debug(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
        subscriber.onSubscribe(this);
    }

    @Override
    public void onWebSocketText(String payload) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", payload);
        }

        Throwable throwable;
        try {
            JsonNode errorJson = jsonMapper.readTree(payload);
            if (errorJson instanceof TextNode) {
                ByteArrayInputStream bin = new ByteArrayInputStream(Base64.getDecoder().decode(payload));
                ObjectInputStream oin = new ObjectInputStream(bin);
                throwable = (Throwable) oin.readObject();
            } else {
                String message = errorJson.path("reason").asText();

                Exception serverException = errorJson.has("exception")
                        ? new Exception(errorJson.path("exception").asText())
                        : null;

                throwable = switch (errorJson.get("status").asInt()) {
                    case 401 -> new NotAuthorizedStreamException(message, serverException);

                    default -> new ServerStreamException(message, serverException);
                };
            }
        } catch (Throwable e) {
            throwable = e;
        }

        if (!isComplete.get()) {
            isComplete.set(true);
            subscriber.onError(throwable);
        }

        // Ack to publisher
        WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
        session.close(StatusCode.NORMAL, "onError", callback);
        callback.future().whenComplete((v, t) -> isDisposed.set(true));
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", new String(payload, offset, len, StandardCharsets.UTF_8));
            }
            Object event = eventReader.readFrom(payload, offset, len);
            subscriber.onNext(event);
            outstandingRequests.decrementAndGet();
            if (requests.get() > 4096)
                sendRequests(0);
        } catch (IOException e) {
            logger.error("Could not receive value", e);

            if (!isComplete.get()) {
                isComplete.set(true);
                subscriber.onError(e);
            }

            WriteCallbackCompletableFuture callback = new WriteCallbackCompletableFuture();
            session.close(StatusCode.NORMAL, "onError", callback);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        this.webSocketError = unwrap(cause);

        if (!(cause instanceof ClosedChannelException)) {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketError", cause);
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        if (statusCode == StatusCode.NORMAL) {
            if (!isComplete.get()) {
                subscriber.onComplete();
            }
            logger.debug(marker, "Complete subscription:{} {}", statusCode, reason);
        } else if (statusCode == StatusCode.BAD_PAYLOAD) {
            if (!isComplete.get()) {
                subscriber.onError(new ClientBadPayloadStreamException(reason));
            }
            logger.warn(marker, "Bad payload:{} {}", statusCode, reason);
        } else if (statusCode == StatusCode.SHUTDOWN) {
            if (!isComplete.get()) {
                if (reason.equals("Connection Idle Timeout")) {
                    logger.debug(marker, "Server timeout:{} {}", statusCode, reason);
                    subscriber.onError(new ServerTimeoutStreamException("Server closed due to timeout"));
                } else {
                    logger.warn(marker, "Server is shutting down:{} {}", statusCode, reason);
                    subscriber.onError(new ServerShutdownStreamException("Server is shutting down"));
                }
            }
        } else {
            logger.warn(marker, "Websocket failed:{} {}", statusCode, reason);
            subscriber.onError(new ServerStreamException("Websocket failed:" + reason));
        }
        if (!isComplete.get()) {
            isComplete.set(true);
        }
        isDisposed.set(true); // Now considered done
    }

    // Send requests
    protected void sendRequests(long n) {
        sendLock.lock();
        try {
            long rn;
            if (isCancelled.get()) {
                rn = Long.MIN_VALUE;
            } else if (n == 0) {
                rn = requests.get();
                if (rn > 4096 && outstandingRequests.get() < 4096) {
                    requests.addAndGet(-4096);
                    rn = 4096;
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
                    }
                }

                if (rn > 4096) {
                    requests.addAndGet(-4096);
                    rn = 4096;
                } else {
                    requests.set(0);
                }
                outstandingRequests.addAndGet(rn);
            }

            String requestString = Long.toString(rn);
            CompletableFuture.runAsync(() ->
            {
                session.getRemote().sendString(requestString, sendWriteCallback);
            });

            if (logger.isTraceEnabled())
                logger.trace(marker, "sendRequest {}", rn);
        } finally {
            sendLock.unlock();
        }
    }

    private class SendWriteCallback
            implements WriteCallback {
        @Override
        public void writeFailed(Throwable x) {
            logger.error(marker, "Could not send requests", x);
        }

        @Override
        public void writeSuccess() {
            // Do nothing
        }
    }
}
