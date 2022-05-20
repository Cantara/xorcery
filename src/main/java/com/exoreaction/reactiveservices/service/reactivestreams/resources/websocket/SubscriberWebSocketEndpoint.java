package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
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
import org.eclipse.jetty.websocket.api.*;
import org.glassfish.jersey.internal.util.collection.ByteBufferInputStream;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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

    private ByteBufferAccumulator byteBufferAccumulator;


    public SubscriberWebSocketEndpoint(ReactiveEventStreams.Subscriber<T> subscriber,
                                       MessageBodyReader<Object> messageBodyReader,
                                       MessageBodyWriter<Object> messageBodyWriter,
                                       ObjectMapper objectMapper,
                                       Type eventType,
                                       Marker marker,
                                       ByteBufferAccumulator byteBufferAccumulator) {
        this.eventType = eventType;
        this.marker = marker;
        this.subscriber = subscriber;
        this.messageBodyReader = messageBodyReader;
        this.messageBodyWriter = messageBodyWriter;
        this.objectMapper = objectMapper;
        this.byteBufferAccumulator = byteBufferAccumulator;
    }

    @Override
    public void onWebSocketPartialBinary(ByteBuffer payload, boolean fin) {
        byteBufferAccumulator.copyBuffer(payload);
        if (fin)
        {
            onWebSocketBinary(byteBufferAccumulator.takeByteBuffer());
            byteBufferAccumulator.close();
        }
    }

    @Override
    public void onWebSocketPartialText(String payload, boolean fin) {
        WebSocketPartialListener.super.onWebSocketPartialText(payload, fin);
    }

    public void onWebSocketBinary(ByteBuffer byteBuffer) {


        try {
            if (metadata == null) {
                metadata = byteBuffer;
            } else {
                ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
                Object event = messageBodyReader.readFrom((Class<Object>) eventType, eventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE, null, inputStream);
                byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
                eventSink.publishEvent((holder, seq, m, e) ->
                {
                    try {
                        holder.metadata = objectMapper.readerForUpdating(holder.metadata).readValue(new ByteBufferBackedInputStream(metadata));
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                    byteBufferAccumulator.getByteBufferPool().release(metadata);
                    if (messageBodyWriter == null) {
                        holder.event = (T) e;
                    } else {
                        CompletableFuture<?> resultFuture = new CompletableFuture<>();
                        resultFuture.whenComplete((result, throwable) ->
                        {
                            // Send result back
                            ByteArrayOutputStream bout = new ByteArrayOutputStream();

                            try {
                                messageBodyWriter.writeTo(result, null, null, null, null, null, bout);
                                session.getRemote().sendBytes(ByteBuffer.wrap(bout.toByteArray()));
                            } catch (IOException ex) {
                                logger.error("Could not send result", ex);
                                session.close();
                            }
                        });

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

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        subscriber.onComplete();
        logger.info("Close websocket:{} {}", statusCode, reason);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        eventSink = subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {

            final AtomicLong requests = new AtomicLong();
            final AtomicReference<CompletableFuture<Void>> sendRequests = new AtomicReference<>();

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
        });

        logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException)
        {
            // Ignore
        } else {
            logger.error("Subscriber websocket error", cause);
        }
    }
}
