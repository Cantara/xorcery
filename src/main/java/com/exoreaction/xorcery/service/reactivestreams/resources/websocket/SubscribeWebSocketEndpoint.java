package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.jetty.client.WriteCallbackCompletableFuture;
import com.exoreaction.xorcery.service.reactivestreams.SubscriptionProcess;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;

public class SubscribeWebSocketEndpoint<T>
        implements WebSocketPartialListener, WebSocketConnectionListener {

    private static final Logger logger = LogManager.getLogger(SubscribeWebSocketEndpoint.class);

    private final ReactiveEventStreams.Subscriber<T> subscriber;
    private MessageBodyReader<Object> messageBodyReader;
    private MessageBodyWriter<Object> messageBodyWriter;
    private ObjectMapper objectMapper;
    private EventSink<Event<T>> eventSink;
    private Session session;
    private Type eventType;
    private Marker marker;

    // Dummy variables for readers
    private final Annotation[] annotations = new Annotation[0];

    private ByteBufferAccumulator byteBufferAccumulator;
    private ByteBufferPool byteBufferPool;
    private SubscriptionProcess<T> subscriptionProcess;
    private String webSocketHref;
    private Configuration publisherConfiguration;
    private Configuration subscriptionConfiguration;

    public SubscribeWebSocketEndpoint(ReactiveEventStreams.Subscriber<T> subscriber,
                                      MessageBodyReader<Object> messageBodyReader,
                                      MessageBodyWriter<Object> messageBodyWriter,
                                      ObjectMapper objectMapper,
                                      Type eventType,
                                      Marker marker,
                                      ByteBufferPool byteBufferPool,
                                      SubscriptionProcess<T> subscriptionProcess,
                                      String webSocketHref,
                                      Configuration publisherConfiguration,
                                      Configuration subscriptionConfiguration) {
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
        this.subscriptionConfiguration = subscriptionConfiguration;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        // First send parameters, if available
        String parameterString = publisherConfiguration.json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAccept(Void ->
                {
                    eventSink = subscriber.onSubscribe(new WebSocketSubscription(session, marker), subscriptionConfiguration);
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

}
