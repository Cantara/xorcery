package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.jetty.client.WriteCallbackCompletableFuture;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsService;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.lmax.disruptor.EventSink;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class SubscriberWebSocketEndpoint<T>
        implements WebSocketPartialListener, WebSocketConnectionListener {

    private static final Logger logger = LogManager.getLogger(SubscriberWebSocketEndpoint.class);

    private final ReactiveEventStreams.Subscriber<T> subscriber;
    private MessageBodyReader<Object> messageBodyReader;
    private MessageBodyWriter<Object> messageBodyWriter;
    private ObjectMapper objectMapper;
    private EventSink<Event<T>> eventSink;
    private ByteBuffer metadata;
    private Session session;
    private Type eventType;
    private Marker marker;

    // Dummy variables for readers
    private final Annotation[] annotations = new Annotation[0];

    private ByteBufferAccumulator byteBufferAccumulator;
    private ByteBufferPool byteBufferPool;
    private ReactiveStreamsService.SubscriptionProcess<T> subscriptionProcess;
    private String webSocketHref;
    private Configuration publisherConfiguration;
    private Timer timer;

    public SubscriberWebSocketEndpoint(ReactiveEventStreams.Subscriber<T> subscriber,
                                       MessageBodyReader<Object> messageBodyReader,
                                       MessageBodyWriter<Object> messageBodyWriter,
                                       ObjectMapper objectMapper,
                                       Type eventType,
                                       Marker marker,
                                       ByteBufferPool byteBufferPool,
                                       ReactiveStreamsService.SubscriptionProcess<T> subscriptionProcess,
                                       String webSocketHref, Configuration publisherConfiguration, Timer timer) {
        this.eventType = eventType;
        this.marker = marker;
        this.subscriber = subscriber;
        this.messageBodyReader = messageBodyReader;
        this.messageBodyWriter = messageBodyWriter;
        this.objectMapper = objectMapper;
        this.byteBufferAccumulator = new ByteBufferAccumulator(byteBufferPool, false);
        this.byteBufferPool = byteBufferPool;
        this.subscriptionProcess = subscriptionProcess;
        this.webSocketHref = webSocketHref;
        this.publisherConfiguration = publisherConfiguration;
        this.timer = timer;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        // First send parameters, if available
        String parameterString = publisherConfiguration.json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAccept(Void ->
                {
                    eventSink = subscriber.onSubscribe(new WebSocketSubscription(session));
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
    public void onWebSocketPartialText(String payload, boolean fin) {
        WebSocketPartialListener.super.onWebSocketPartialText(payload, fin);
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
            if (metadata == null) {
                metadata = byteBuffer;
            } else {
//                logger.info(marker, "Received:"+ Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));

                ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
                Object event = messageBodyReader.readFrom((Class<Object>) eventType, eventType, annotations, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, inputStream);
                byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
                eventSink.publishEvent((holder, seq, m, e) ->
                {
                    try {
                        holder.metadata = objectMapper.readValue(new ByteBufferBackedInputStream(metadata), Metadata.class);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                    byteBufferAccumulator.getByteBufferPool().release(metadata);
                    if (messageBodyWriter == null) {
                        holder.event = (T) e;
                    } else {
                        CompletableFuture<?> resultFuture = new CompletableFuture<>();
                        resultFuture.whenComplete(this::sendResult);

                        holder.event = (T) new EventWithResult<>(e, resultFuture);
                    }
                }, metadata, event);
                metadata = null;
            }
        } catch (IOException e) {
            logger.error("Could not receive value", e);
            session.close();
        }
    }

    private void sendResult(Object result, Throwable throwable) {
        // Send result back
        ByteBufferOutputStream2 resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);

        try {
            if (throwable != null) {
                ObjectOutputStream out = new ObjectOutputStream(resultOutputStream);
                out.writeObject(throwable);
            } else {
                messageBodyWriter.writeTo(result, null, null, annotations, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, resultOutputStream);
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
            subscriber.onError(ex);
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
        } else {
            logger.error(marker, "Subscriber websocket error", cause);
            subscriber.onError(cause);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            logger.info(marker, "Complete subscription to {}:{} {}", webSocketHref, statusCode, reason);
            subscriber.onComplete();
            subscriptionProcess.result().complete(null); // Now considered done
        } else {
            logger.info(marker, "Close websocket {}:{} {}", webSocketHref, statusCode, reason);
            logger.info(marker, "Starting subscription process again");
            subscriptionProcess.retry();
        }
    }

    private class WebSocketSubscription implements ReactiveEventStreams.Subscription {

        final AtomicLong requests;
        final AtomicReference<CompletableFuture<Void>> sendRequests;
        private final Session session;

        public WebSocketSubscription(Session session) {
            this.session = session;
            requests = new AtomicLong();
            sendRequests = new AtomicReference<>();
        }

        @Override
        public void request(long n) {
            if (!session.isOpen())
                return;

            requests.addAndGet(n);

            if (sendRequests.get() == null) {
                sendRequests.set(new CompletableFuture<>());
                sendRequests.get().whenComplete((v, t) ->
                {
                    sendRequests.set(null);
                    long rn = requests.getAndSet(0);
                    if (rn > 0) {
                        request(rn);
                    }
                });

                long rn = requests.getAndSet(0);
                session.getRemote().sendString(Long.toString(rn), new WriteCallback() {
                    @Override
                    public void writeFailed(Throwable x) {
                        logger.error(marker, "Could not send request", x);
                        sendRequests.get().completeExceptionally(x);
                    }

                    @Override
                    public void writeSuccess() {
                        sendRequests.get().complete(null);
                    }
                });
            }
        }

        @Override
        public void cancel() {
            requests.set(0);
            request(Long.MIN_VALUE);
        }
    }
}
