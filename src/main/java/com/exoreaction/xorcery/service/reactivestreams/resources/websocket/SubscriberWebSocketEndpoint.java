package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class SubscriberWebSocketEndpoint<T>
        implements WebSocketPartialListener, WebSocketConnectionListener {
    private final static Logger logger = LogManager.getLogger(SubscriberWebSocketEndpoint.class);
    private Session session;
    private Semaphore semaphore = new Semaphore(0);
    private String webSocketPath;
    private ReactiveEventStreams.Subscriber<T> subscriber;
    private String configurationMessage = "";
    private Configuration subscriberConfiguration;
    private MessageBodyWriter<Object> messageBodyWriter;
    private MessageBodyReader<Object> messageBodyReader;

    // Dummy variables for readers
    private final Annotation[] annotations = new Annotation[0];

    private EventSink<Event<T>> eventSink;

    private ByteBufferAccumulator byteBufferAccumulator;
    private ByteBufferPool byteBufferPool;

    private Type eventType;
    private final ObjectMapper objectMapper;
    private Marker marker;

    public SubscriberWebSocketEndpoint(String webSocketPath,
                                       ReactiveEventStreams.Subscriber<T> subscriber,
                                       MessageBodyWriter<Object> messageBodyWriter,
                                       MessageBodyReader<Object> messageBodyReader,
                                       ObjectMapper objectMapper,
                                       Type eventType,
                                       ByteBufferPool byteBufferPool,
                                       Marker marker) {
        this.webSocketPath = webSocketPath;
        this.subscriber = subscriber;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.eventType = eventType;
        this.objectMapper = objectMapper;
        this.byteBufferPool = byteBufferPool;
        this.byteBufferAccumulator = new ByteBufferAccumulator(byteBufferPool, false);
        this.marker = marker;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public synchronized void onWebSocketPartialText(String message, boolean fin) {
        if (subscriberConfiguration == null) {
            configurationMessage += message;

            if (fin) {
                // Read JSON parameters
                try {
                    subscriberConfiguration = new Configuration((ObjectNode) objectMapper.readTree(configurationMessage));
                    eventSink = subscriber.onSubscribe(new WebSocketSubscription(session, marker), subscriberConfiguration);

                    logger.info(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());

                } catch (JsonProcessingException e) {
                    session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
                }
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

    private void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            logger.debug(marker, "Received:" + Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));
            JsonFactory jf = new JsonFactory(objectMapper);
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            JsonParser jp = jf.createParser(inputStream);
            JsonToken metadataToken = jp.nextToken();
            Metadata metadata = jp.readValueAs(Metadata.class);
            long location = jp.getCurrentLocation().getByteOffset();
            byteBuffer.position((int) location);

            Object event = messageBodyReader.readFrom((Class<Object>) eventType, eventType, annotations, MediaType.APPLICATION_OCTET_STREAM_TYPE, null, inputStream);
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
            eventSink.publishEvent((holder, seq, m, e) ->
            {
                holder.metadata = m;
                if (messageBodyWriter == null) {
                    holder.event = (T) e;
                } else {
                    CompletableFuture<?> resultFuture = new CompletableFuture<>();
                    resultFuture.whenComplete(this::sendResult);

                    holder.event = (T) new EventWithResult<>(e, resultFuture);
                }
            }, metadata, event);
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
        logger.info(marker, "Complete subscription to {}:{} {}", webSocketPath, statusCode, reason);
        subscriber.onComplete();
    }
}
