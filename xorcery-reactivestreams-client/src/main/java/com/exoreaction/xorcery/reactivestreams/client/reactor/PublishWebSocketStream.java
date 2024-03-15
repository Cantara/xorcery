package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry;
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
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;

public class PublishWebSocketStream<T>
        extends BaseSubscriber<T>
        implements
        Subscription,
        WebSocketListener,
        WriteCallback {

    private static final TextMapSetter<? super ClientUpgradeRequest> jettySetter =
            (carrier, key, value) -> carrier.setHeader(key, value);

    private final URI serverUri;
    private final String mediaType;
    protected final MessageWriter<T> writer;

    private final Publisher<T> publisher;
    private final Subscriber<? super T> subscriber;

    private final WebSocketClientOptions options;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;

    private final Logger logger;
    protected final Marker marker;
    protected final Span span;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;

    protected final ByteBufferPool pool;

    private boolean redundancyNotificationIssued = false;

    // All the state which requires synchronized access to session
    protected Session session;
    private Iterator<URI> uriIterator;

    private final BlockingArrayQueue<Object> sendQueue = new BlockingArrayQueue<>(4096, 1024);
    private final AtomicBoolean isDraining = new AtomicBoolean();
    private final Lock drainLock = new ReentrantLock();

    protected final ByteBufferOutputStream2 outputStream;

    protected AtomicBoolean isComplete = new AtomicBoolean();

    protected final Attributes attributes;
    protected final LongHistogram sentBytes;
    protected final LongHistogram requestsHistogram;
    protected final LongHistogram flushHistogram;

    public PublishWebSocketStream(
            URI serverUri,
            String mediaType,
            MessageWriter<T> writer,
            Publisher<T> publisher,
            Subscriber<? super T> subscriber,
            WebSocketClientOptions options,

            DnsLookup dnsLookup,
            WebSocketClient webSocketClient,

            ByteBufferPool pool,
            Meter meter,
            Tracer tracer,
            TextMapPropagator textMapPropagator,
            Logger logger) {
        this.serverUri = serverUri;
        this.mediaType = mediaType;
        this.publisher = publisher;
        this.subscriber = subscriber;
        this.options = options;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.writer = writer;
        this.pool = pool;
        this.tracer = tracer;
        this.textMapPropagator = textMapPropagator;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(serverUri.toASCIIString());
        this.outputStream = new ByteBufferOutputStream2(pool, false);

        this.attributes = Attributes.builder()
                .put(SemanticAttributes.MESSAGING_SYSTEM, ReactiveStreamsOpenTelemetry.XORCERY_MESSAGING_SYSTEM)
                .put(SemanticAttributes.URL_FULL, serverUri.toASCIIString())
                .build();
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();

        span = tracer.spanBuilder(serverUri.toASCIIString() + " publisher")
                .setSpanKind(SpanKind.PRODUCER)
                .setAllAttributes(attributes)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            start();
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

    private CompletableFuture<T> connect(Iterator<URI> subscriberURIs) {
        CompletableFuture<T> failure = null;
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
                clientUpgradeRequest.setHeader(HttpHeader.CONTENT_TYPE.asString(), mediaType);
                clientUpgradeRequest.setCookies(options.cookies());
                clientUpgradeRequest.setHeaders(options.headers());
                clientUpgradeRequest.setSubProtocols(options.subProtocols());
                clientUpgradeRequest.setExtensions(options.extensions().stream().map(ExtensionConfig::parse).toList());
                textMapPropagator.inject(Context.current(), clientUpgradeRequest, jettySetter);
                webSocketClient.connect(this, subscriberWebsocketUri, clientUpgradeRequest)
                        .thenAccept(this::connected)
                        .thenRun(connectSpan::end).join();

                // Subscribe upstream
                publisher.subscribe(this);
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

    private void connected(Session session) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "connected");
        }
        this.session = session;
    }



    // Subscriber
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        // Publish downstream
        subscriber.onSubscribe(this);
    }

    @Override
    protected void hookOnNext(T item) {
        try {
            send(item);
        } catch (IOException e) {
            subscriber.onError(e);
        }
    }

    @Override
    protected void hookOnComplete() {
        if (session.isOpen())
        {
            session.close(StatusCode.NORMAL, "complete");
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        if (session.isOpen())
        {
            session.close(StatusCode.SHUTDOWN, "error");
        }
        subscriber.onError(throwable);
    }

    @Override
    protected void hookOnCancel() {
        upstream().cancel();
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {

        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());
        }

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.AUTO);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketText {}", message);
        }

        long requestAmount = Long.parseLong(message);

        if (requestAmount == Long.MIN_VALUE) {
            logger.debug(marker, "Received cancel on websocket");
            session.close(StatusCode.NORMAL, "cancel");
            upstream().cancel();
            subscriber.onError(new ServerShutdownStreamException("cancel"));
        } else {
            request(requestAmount);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {

        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (logger.isDebugEnabled()) {
            logger.debug(marker, "onWebSocketError", cause);
        }
        subscriber.onError(cause);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);
        }

        if (statusCode == StatusCode.NORMAL) {
            logger.debug(marker, "Session closed:{} {}", statusCode, reason);
            subscriber.onComplete();
        } else if (statusCode == StatusCode.SHUTDOWN || statusCode == StatusCode.NO_CLOSE) {
            logger.debug(marker, "Close websocket:{} {}", statusCode, reason);
        }
    }

    protected void send(T item) throws IOException {
//        if (logger.isTraceEnabled())
//            logger.trace(marker, "send {}", item.getClass().toString());

        // Write event data
        writeItem(writer, item, outputStream);
        ByteBuffer eventBuffer = outputStream.takeByteBuffer();
        sentBytes.record(eventBuffer.limit(), attributes);
        session.getRemote().sendBytes(eventBuffer);
        pool.release(eventBuffer);
    }

    protected void writeItem(MessageWriter<T> messageWriter, T item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }
}
