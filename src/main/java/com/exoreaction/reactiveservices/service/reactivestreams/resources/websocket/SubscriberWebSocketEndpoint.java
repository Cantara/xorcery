package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventSink;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class SubscriberWebSocketEndpoint<T>
        implements WebSocketListener {

    private static final Logger logger = LogManager.getLogger(SubscriberWebSocketEndpoint.class);

    private final ReactiveEventStreams.Subscriber<T> subscriber;
    private MessageBodyReader<Object> messageBodyReader;
    private MessageBodyWriter<Object> messageBodyWriter;
    private ObjectMapper objectMapper;
    private EventSink<Event<T>> eventSink;
    private Metadata metadata;
    private Session session;
    private Type eventType;
    private Marker marker;

    public SubscriberWebSocketEndpoint(ReactiveEventStreams.Subscriber<T> subscriber,
                                       MessageBodyReader<Object> messageBodyReader,
                                       MessageBodyWriter<Object> messageBodyWriter,
                                       ObjectMapper objectMapper,
                                       Type eventType,
                                       Marker marker) {
        this.eventType = eventType;
        this.marker = marker;
        this.subscriber = subscriber;
        this.messageBodyReader = messageBodyReader;
        this.messageBodyWriter = messageBodyWriter;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onWebSocketText(String message) {
        logger.info("Receive text:" + message);
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            if (metadata == null) {
                metadata = objectMapper.readValue(payload, offset, len, Metadata.class);
            } else {
                ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
                Object event = messageBodyReader.readFrom((Class<Object>)eventType, eventType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE, new MultivaluedStringMap(), bin);

                eventSink.publishEvent((holder, seq, m, e) ->
                {
                    holder.metadata = m;
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
        logger.info("Close websocket:{} {}", statusCode, reason);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        eventSink = subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                try {
                    session.getRemote().sendString(Long.toString(n));
                } catch (IOException e) {
                    logger.error(marker, "Could not send request", e);
                }
            }

            @Override
            public void cancel() {
                session.close();
            }
        });

        logger.info(marker, "Connected to {}", session.getUpgradeRequest().getRequestURI());
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        logger.error("Web socket error", cause);
    }
}
