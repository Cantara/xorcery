package com.exoreaction.xorcery.service.reactivestreams.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.WriteCallbackCompletableFuture;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.util.ByteBufferBackedInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class SubscribeReactiveStream
        implements WebSocketPartialListener,
        WebSocketConnectionListener,
        Flow.Subscription {

    private static final Logger logger = LogManager.getLogger(SubscribeReactiveStream.class);

    private final String authority;
    private final String streamName;
    private Configuration subscriberConfiguration;
    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;
    private final Flow.Subscriber<Object> subscriber;
    private final MessageReader<Object> eventReader;
    private final MessageWriter<Object> resultWriter;
    private final Supplier<Configuration> publisherConfiguration;
    private final ScheduledExecutorService timer;
    private final ByteBufferPool byteBufferPool;
    private final CompletableFuture<Void> result;

    private final Marker marker;
    private final ByteBufferAccumulator byteBufferAccumulator;
    private final AtomicLong requests = new AtomicLong();
    private final AtomicBoolean cancelled = new AtomicBoolean();

    private Iterator<URI> uriIterator;
    private Session session;

    public SubscribeReactiveStream(String authority,
                                   String streamName,
                                   Configuration subscriberConfiguration,
                                   DnsLookup dnsLookup,
                                   WebSocketClient webSocketClient,
                                   Flow.Subscriber<Object> subscriber,
                                   MessageReader<Object> eventReader,
                                   MessageWriter<Object> resultWriter,
                                   Supplier<Configuration> publisherConfiguration,
                                   ScheduledExecutorService timer,
                                   ByteBufferPool pool,
                                   CompletableFuture<Void> result) {
        this.authority = authority;
        this.streamName = streamName;
        this.subscriberConfiguration = subscriberConfiguration;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.subscriber = subscriber;
        this.eventReader = eventReader;
        this.resultWriter = resultWriter;
        this.publisherConfiguration = publisherConfiguration;
        this.timer = timer;
        this.byteBufferAccumulator = new ByteBufferAccumulator(pool, false);
        this.byteBufferPool = pool;
        this.result = result;
        this.marker = MarkerManager.getMarker(authority + ":" + streamName);

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

        URI uri = URI.create("ws://" + authority + "/streams/publishers/" + streamName);
        dnsLookup.resolve(uri).thenApply(list ->
        {
            this.uriIterator = list.iterator();
            return uriIterator;
        }).thenAccept(this::connect).exceptionally(this::exceptionally);
    }

    private void connect(Iterator<URI> subscriberURIs) {

        if (subscriberURIs.hasNext()) {
            URI subscriberWebsocketUri = subscriberURIs.next();
            try {
                webSocketClient.connect(this, subscriberWebsocketUri)
                        .thenAccept(this::connected)
                        .exceptionally(this::exceptionally);
            } catch (Throwable e) {
                logger.error(marker, "Could not subscribe to " + subscriberWebsocketUri.toASCIIString(), e);
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
        // Ignore
    }

    @Override
    public void onWebSocketPartialBinary(ByteBuffer payload, boolean fin) {
        byteBufferAccumulator.copyBuffer(payload);
        if (fin) {
            onWebSocketBinary(byteBufferAccumulator.takeByteBuffer());
            byteBufferAccumulator.close();
        }
    }

    private void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
//                logger.info(marker, "Received:"+ Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));
            if (eventReader != null) {
                ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
                Object event = eventReader.readFrom(inputStream);
                byteBufferAccumulator.getByteBufferPool().release(byteBuffer);

                if (resultWriter != null) {
                    event = new WithResult<>(event, new CompletableFuture<>().whenComplete(this::sendResult));
                }
                subscriber.onNext(event);
            } else {
                ByteBuffer result = ByteBuffer.allocate(byteBuffer.limit());
                result.put(byteBuffer);
                result.flip();
                byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
                subscriber.onNext(result);
            }
        } catch (IOException e) {
            logger.error("Could not receive value", e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            result.completeExceptionally(e);
        }
    }

    private void sendResult(Object result, Throwable throwable) {
        // Send result back
        ByteBufferOutputStream2 resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);

        try {
            if (throwable != null) {
                resultOutputStream.write(ReactiveStreamsAbstractService.XOR);
                ObjectOutputStream out = new ObjectOutputStream(resultOutputStream);
                out.writeObject(throwable);
            } else {
                resultWriter.writeTo(result, resultOutputStream);
            }

            ByteBuffer data = resultOutputStream.takeByteBuffer();
            session.getRemote().sendBytes(data, new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    logger.error(marker, "Could not send result", x);
                }

                @Override
                public void writeSuccess() {
                    byteBufferPool.release(data);
                }
            });
        } catch (IOException ex) {
            logger.error(marker, "Could not send result", ex);
            session.close(StatusCode.SERVER_ERROR, ex.getMessage());
            this.result.completeExceptionally(ex); // TODO This should probably do a retry instead
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
            retry();
        } else {
            logger.error(marker, "Subscriber websocket error", cause);
            retry();
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {

        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            try {
                subscriber.onComplete();
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
