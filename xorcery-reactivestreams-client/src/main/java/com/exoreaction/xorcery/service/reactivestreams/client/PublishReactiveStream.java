package com.exoreaction.xorcery.service.reactivestreams.client;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.net.ssl.SSLHandshakeException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * @author rickardoberg
 */
public class PublishReactiveStream
        implements WebSocketListener,
        Flow.Subscriber<Object>,
        EventHandler<AtomicReference<Object>> {

    private final static Logger logger = LogManager.getLogger(PublishReactiveStream.class);

    private final static Pattern uriStartsWithSchemeAndAuthorityPattern = Pattern.compile(".+://.+");

    protected Session session;

    private Configuration publisherConfiguration;
    private DnsLookup dnsLookup;
    private WebSocketClient webSocketClient;
    private final Flow.Publisher<Object> publisher;
    protected Flow.Subscription subscription;
    protected final Semaphore semaphore = new Semaphore(0);

    protected final MessageWriter<Object> eventWriter;

    private final Supplier<Configuration> subscriberConfiguration;
    private ScheduledExecutorService timer;
    protected final ByteBufferPool pool;
    protected CompletableFuture<Void> result;
    protected final Marker marker;

    protected Meter sent;
    protected Histogram sentBatchSize;
    protected long batchSize;

    private boolean redundancyNotificationIssued = false;

    protected Disruptor<AtomicReference<Object>> disruptor;
    private final String scheme;
    private final String authority;
    private final String streamName;
    private Iterator<URI> uriIterator;

    public PublishReactiveStream(String defaultScheme,
                                 String authorityOrBaseUri,
                                 String streamName,
                                 Configuration publisherConfiguration,
                                 DnsLookup dnsLookup,
                                 WebSocketClient webSocketClient,
                                 Flow.Publisher<Object> publisher,
                                 MessageWriter<Object> eventWriter,
                                 Supplier<Configuration> subscriberConfiguration,
                                 ScheduledExecutorService timer,
                                 ByteBufferPool pool,
                                 MetricRegistry metricRegistry,
                                 CompletableFuture<Void> result) {
        this.streamName = streamName;
        this.publisherConfiguration = publisherConfiguration;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.publisher = publisher;
        this.eventWriter = eventWriter;
        this.subscriberConfiguration = subscriberConfiguration;
        this.timer = timer;
        this.pool = pool;
        this.result = result;
        this.marker = MarkerManager.getMarker(authorityOrBaseUri + "/" + streamName);
        this.sent = metricRegistry.meter("publish.sent." + streamName);
        this.sentBatchSize = metricRegistry.histogram("publish.sent.batchsize." + streamName);

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
            this.scheme = publisherConfiguration.getString("reactivestreams.client.scheme").orElse(defaultScheme);
        }

        this.disruptor = new Disruptor<>(AtomicReference::new, publisherConfiguration.getInteger("disruptor.size").orElse(512), new NamedThreadFactory("PublishWebSocketDisruptor-" + marker.getName() + "-"));
        disruptor.handleEventsWith(this);
        disruptor.start();
        start();
    }

    // Subscriber
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onNext(Object item) {
        disruptor.publishEvent((e, s, event) ->
        {
            e.set(event);
        }, item);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.info(marker, "Subscriber error for {}", session.getRemote().getRemoteAddress(), throwable);
        disruptor.shutdown();
        logger.info(marker, "Sending close for session {}", session.getRemote().getRemoteAddress());
        session.close(StatusCode.SERVER_ERROR, throwable.getMessage());
        result.completeExceptionally(throwable);
    }

    public void onComplete() {
        CompletableFuture.runAsync(() ->
        {
            logger.info(marker, "Waiting for outstanding events to be sent to {}", session.getRemote().getRemoteAddress());
            disruptor.shutdown();
            result.complete(null);
            logger.info(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
            session.close(1000, "complete");
        });
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
        }).thenAccept(this::connect).whenComplete(this::exceptionally);
    }

    private void connect(Iterator<URI> subscriberURIs) {

        if (subscriberURIs.hasNext()) {
            URI subscriberWebsocketUri = subscriberURIs.next();
            URI schemaAdjustedSubscriberWebsocketUri = subscriberWebsocketUri;
            if (subscriberWebsocketUri.getScheme() != null) {
                if (subscriberWebsocketUri.getScheme().equals("https")) {
                    try {
                        schemaAdjustedSubscriberWebsocketUri = new URI(
                                "wss",
                                subscriberWebsocketUri.getAuthority(),
                                subscriberWebsocketUri.getPath(),
                                subscriberWebsocketUri.getQuery(),
                                subscriberWebsocketUri.getFragment()
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else if (subscriberWebsocketUri.getScheme().equals("http")) {
                    try {
                        schemaAdjustedSubscriberWebsocketUri = new URI(
                                "ws",
                                subscriberWebsocketUri.getAuthority(),
                                subscriberWebsocketUri.getPath(),
                                subscriberWebsocketUri.getQuery(),
                                subscriberWebsocketUri.getFragment()
                        );
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            URI effectiveSubscriberWebsocketUri = URI.create(schemaAdjustedSubscriberWebsocketUri.getScheme() + "://" + schemaAdjustedSubscriberWebsocketUri.getAuthority() + "/streams/subscribers/" + streamName);
            logger.info(marker, "Trying " + effectiveSubscriberWebsocketUri);
            try {
                webSocketClient.connect(this, effectiveSubscriberWebsocketUri)
                        .whenComplete(this::exceptionally);
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + effectiveSubscriberWebsocketUri.toASCIIString(), e);
                retry();
            }
        } else {
            retry();
        }
    }

    private <T> void exceptionally(T value, Throwable throwable) {
        if (throwable != null) {
            logger.error(marker, "Publish reactive stream error", throwable);
            if (throwable instanceof SSLHandshakeException) {
                // Give up
                result.completeExceptionally(throwable);
            } else {
                // TODO Handle more exceptions
                retry();
            }
        }
    }

    public void retry() {
        if (result.isDone()) {
            return;
        }

        // TODO Turn this into exponential backoff
        if (!uriIterator.hasNext())
            timer.schedule(this::start, 10000, TimeUnit.MILLISECONDS);
        else
            connect(uriIterator);
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        // First send parameters, if available
        String parameterString = subscriberConfiguration.get().json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAccept(Void ->
                {
                    logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
                }).exceptionally(t ->
                {
                    session.close(StatusCode.SERVER_ERROR, t.getMessage());
                    logger.error(marker, "Parameter handshake failed", t);
                    return null;
                })
        ));
    }

    @Override
    public void onWebSocketText(String message) {

        long requestAmount = Long.parseLong(message);

        if (requestAmount == Long.MIN_VALUE) {
            logger.info(marker, "Received cancel on websocket");
            session.close();
            session = null;
            retry();
        } else {
            if (logger.isDebugEnabled())
                logger.debug(marker, "Received request:" + requestAmount);

            if (subscription == null) {
                publisher.subscribe(this);
            }

            semaphore.release((int) requestAmount);
            subscription.request(requestAmount);
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
    public void onWebSocketClose(int statusCode, String reason) {

        session = null;
        // TODO This has to be reviewed to see what codes should cause retry and what should cause cancellation of the process
        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            logger.info(marker, "Session closed:{} {}", statusCode, reason);
            result.complete(null);
        } else {
            logger.info(marker, "Close websocket:{} {}", statusCode, reason);
            logger.info(marker, "Starting publishing process again");
            retry();
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
            if (!result.isDone()) {
                retry();
            }
        } else {
            logger.error(marker, "Publisher websocket error", cause);
            if (subscription != null)
                subscription.cancel();
            result.completeExceptionally(cause); // Now considered done
        }
    }

    // EventHandler
    AtomicReference<Boolean> isShutdown = new AtomicReference<>(false);

    @Override
    public void onShutdown() {
        isShutdown.set(true);
    }

    @Override
    public void onEvent(AtomicReference<Object> event, long sequence, boolean endOfBatch) throws Exception {
        try {
            while (!semaphore.tryAcquire(1, TimeUnit.SECONDS) && !result.isDone()) {
                if (session == null || !session.isOpen())
                    return;
            }

            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write event data
            try {
                Object item = event.get();
                eventWriter.writeTo(item, outputStream);
            } catch (Throwable t) {
                logger.error(marker, "Could not send event", t);
                subscription.cancel();
            }

            // Send it
            ByteBuffer eventBuffer = outputStream.takeByteBuffer();

            session.getRemote().sendBytes(eventBuffer, new WriteCallback() {
                @Override
                public void writeFailed(Throwable t) {
                    pool.release(eventBuffer);
                    onWebSocketError(t);
                }

                @Override
                public void writeSuccess() {
                    pool.release(eventBuffer);
                }
            });

            sent.mark();
            if (endOfBatch) {
                session.getRemote().flush();
                sentBatchSize.update(batchSize);
            }
        } catch (Throwable e) {
            if (session != null)
                throw new RuntimeException(e);
        }
    }

    @Override
    public void onBatchStart(long batchSize) {
        this.batchSize = batchSize;
    }
}
