package com.exoreaction.xorcery.service.reactivestreams.client;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.util.ByteBufferBackedInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.exoreaction.xorcery.util.Exceptions.unwrap;

public class SubscribeReactiveStream
        implements WebSocketPartialListener,
        WebSocketConnectionListener,
        Flow.Subscription {

    private static final Logger logger = LogManager.getLogger(SubscribeReactiveStream.class);

    private final static Pattern uriStartsWithSchemeAndAuthorityPattern = Pattern.compile(".+://.+");

    private final String scheme;
    private final String authority;
    private final String streamName;
    private Configuration subscriberConfiguration;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;
    protected final Flow.Subscriber<Object> subscriber;
    protected final MessageReader<Object> eventReader;
    private final Supplier<Configuration> publisherConfiguration;
    private final ScheduledExecutorService timer;
    protected final ByteBufferPool byteBufferPool;
    protected final CompletableFuture<Void> result;

    protected final Marker marker;
    protected final ByteBufferAccumulator byteBufferAccumulator;
    private final AtomicLong requests = new AtomicLong();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private String exceptionPayload = "";

    private Iterator<URI> uriIterator;
    protected Session session;

    protected final Meter received;
    protected final Meter receivedBytes;
    protected final Histogram requestsHistogram;

    public SubscribeReactiveStream(String defaultScheme,
                                   String authorityOrBaseUri,
                                   String streamName,
                                   Configuration subscriberConfiguration,
                                   DnsLookup dnsLookup,
                                   WebSocketClient webSocketClient,
                                   Flow.Subscriber<Object> subscriber,
                                   MessageReader<Object> eventReader,
                                   Supplier<Configuration> publisherConfiguration,
                                   ScheduledExecutorService timer,
                                   ByteBufferPool pool,
                                   MetricRegistry metricRegistry,
                                   CompletableFuture<Void> result) {
        this.streamName = streamName;
        this.subscriberConfiguration = subscriberConfiguration;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.subscriber = subscriber;
        this.eventReader = eventReader;
        this.publisherConfiguration = publisherConfiguration;
        this.timer = timer;
        this.byteBufferAccumulator = new ByteBufferAccumulator(pool, false);
        this.byteBufferPool = pool;

        this.received = metricRegistry.meter("subscribe." + streamName + ".received");
        this.receivedBytes = metricRegistry.meter("subscribe." + streamName + ".received.bytes");
        this.requestsHistogram = metricRegistry.histogram("subscribe."+streamName+".requests");

        this.result = result;

        if (uriStartsWithSchemeAndAuthorityPattern.matcher(authorityOrBaseUri).matches()) {
            URI uri = URI.create(authorityOrBaseUri);
            this.scheme = uri.getScheme();
            if (uri.getPath() != null) {
                this.authority = uri.getAuthority() + uri.getPath();
            } else {
                this.authority = uri.getAuthority();
            }
        } else {
            this.authority = authorityOrBaseUri;
            this.scheme = subscriberConfiguration.getString("reactivestreams.client.scheme").orElse(defaultScheme);
        }

        this.marker = MarkerManager.getMarker(this.authority + "/" + streamName);

        this.result.exceptionally(throwable ->
        {
            // Check if session is open
            if (session != null)
                session.close();

            subscriber.onError(throwable);

            return null;
        });

        subscriber.onSubscribe(this);

        start();
    }

    // Subscription
    @Override
    public void request(long n) {
        logger.trace(marker, "request() called: {}", n);
        if (cancelled.get()) {
            return;
        }
        requests.addAndGet(n);
        if (session != null)
            timer.execute(this::sendRequests);
    }

    @Override
    public void cancel() {
        logger.trace(marker, "cancel() called");
        cancelled.set(true);
        requests.set(Long.MIN_VALUE);
        if (session != null)
            timer.execute(this::sendRequests);
    }

    // Connection process
    public void start() {
        if (result.isDone()) {
            return;
        }

        if (!webSocketClient.isStarted()) {
            retry();
        }

        URI uri = URI.create(scheme + "://" + authority);
        logger.info(marker, "Resolving " + uri);
        dnsLookup.resolve(uri).thenApply(list ->
        {
            this.uriIterator = list.iterator();
            return uriIterator;
        }).thenAccept(this::connect).exceptionally(this::exceptionally);
    }

    private void connect(Iterator<URI> publisherURIs) {

        if (publisherURIs.hasNext()) {
            URI publisherWebsocketUri = publisherURIs.next();
            URI schemaAdjustedPublishedWebsocketUri = publisherWebsocketUri;
            if (publisherWebsocketUri.getScheme() != null) {
                if (publisherWebsocketUri.getScheme().equals("https")) {
                    try {
                        schemaAdjustedPublishedWebsocketUri = new URI(
                                "wss",
                                publisherWebsocketUri.getAuthority(),
                                publisherWebsocketUri.getPath(),
                                publisherWebsocketUri.getQuery(),
                                publisherWebsocketUri.getFragment()
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else if (publisherWebsocketUri.getScheme().equals("http")) {
                    try {
                        schemaAdjustedPublishedWebsocketUri = new URI(
                                "ws",
                                publisherWebsocketUri.getAuthority(),
                                publisherWebsocketUri.getPath(),
                                publisherWebsocketUri.getQuery(),
                                publisherWebsocketUri.getFragment()
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            URI effectivePublisherWebsocketUri = URI.create(schemaAdjustedPublishedWebsocketUri.getScheme() + "://" + schemaAdjustedPublishedWebsocketUri.getAuthority() + "/streams/publishers/" + streamName);
            logger.info(marker, "Trying " + effectivePublisherWebsocketUri);
            try {
                webSocketClient.connect(this, effectivePublisherWebsocketUri)
                        .thenAccept(this::connected)
                        .exceptionally(this::exceptionally);
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + effectivePublisherWebsocketUri.toASCIIString(), e);
                retry();
            }
        } else {
            retry();
        }
    }

    private Void exceptionally(Throwable throwable) {
        // TODO Handle exceptions
        logger.error(marker, "Subscribe reactive stream error", throwable);
        retry();
        return null;
    }

    private void connected(Session session) {
        this.session = session;
    }

    public void retry() {
        if (!uriIterator.hasNext())
            timer.schedule(this::start, subscriberConfiguration.getInteger("retry").orElse(10), TimeUnit.SECONDS);
        else
            connect(uriIterator);
    }

    // Websocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        // First send parameters, if available
        String parameterString = publisherConfiguration.get().json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAcceptAsync(Void ->
                {
                    logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
                    timer.submit(this::sendRequests);
                }).exceptionally(t ->
                {
                    session.close(StatusCode.SERVER_ERROR, t.getMessage());
                    logger.error(marker, "Parameter handshake failed", t);
                    return null;
                })
        ));
    }

    @Override
    public void onWebSocketPartialText(String payload, boolean fin) {

        exceptionPayload += payload;
        if (fin) {
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(Base64.getDecoder().decode(exceptionPayload));
                ObjectInputStream oin = new ObjectInputStream(bin);
                Throwable throwable = (Throwable) oin.readObject();
                result.completeExceptionally(throwable);
            } catch (Throwable e) {
                result.completeExceptionally(e);
            }
        }
    }

    @Override
    public void onWebSocketPartialBinary(ByteBuffer payload, boolean fin) {
        byteBufferAccumulator.copyBuffer(payload);
        if (fin) {
            onWebSocketBinary(byteBufferAccumulator.takeByteBuffer());
            byteBufferAccumulator.close();
        }
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            if (logger.isDebugEnabled())
            {
                logger.debug(marker, "Received:" + Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));
            }

            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            received.mark();
            receivedBytes.mark(byteBuffer.position());
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
            subscriber.onNext(event);
        } catch (IOException e) {
            logger.error("Could not receive value", e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            result.completeExceptionally(e);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        cause = unwrap(cause);

        if (cause instanceof ClosedChannelException) {
            // Ignore
            subscriber.onComplete();
            result.cancel(false);
        } else {
            logger.error(marker, "Subscriber websocket error", cause);
            retry();
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            try {
                if (!result.isCompletedExceptionally()) {
                    subscriber.onComplete();
                }
            } catch (Exception e) {
                logger.warn(marker, "Could not close subscription", e);
            }
            logger.info(marker, "Complete subscription:{} {}", statusCode, reason);
            result.complete(null); // Now considered done
        } else {
            logger.warn(marker, "Closed websocket:{} {}", statusCode, reason);
            logger.info(marker, "Starting subscription process again");
            retry();
        }
    }

    // Send requests
    public void sendRequests() {
        try {
            if (!session.isOpen()) {
                logger.info(marker, "Session closed, cannot send requests");
                return;
            }

            final long rn = requests.getAndSet(0);
            if (rn == 0) {
                if (cancelled.get()) {
                    logger.info(marker, "Subscription cancelled");
                }
                return;
            }

            logger.trace(marker, "Sending request: {}", rn);
            requestsHistogram.update(rn);
            session.getRemote().sendString(Long.toString(rn), new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    logger.error(marker, "Could not send requests {}", rn, x);
                }

                @Override
                public void writeSuccess() {
                    logger.trace(marker, "Successfully sent request {}", rn);
                }
            });

            try {
                logger.trace(marker, "Flushing remote session...");
                session.getRemote().flush();
                logger.trace(marker, "Remote session flushed.");
            } catch (IOException e) {
                logger.error(marker, "While flushing remote session", e);
            }
        } catch (Throwable t) {
            logger.error(marker, "Error sending requests", t);
        }
    }
}
