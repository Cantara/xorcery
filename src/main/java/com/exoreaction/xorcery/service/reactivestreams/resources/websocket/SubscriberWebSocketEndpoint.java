package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
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
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class SubscriberWebSocketEndpoint
        implements WebSocketPartialListener, WebSocketConnectionListener {
    private final static Logger logger = LogManager.getLogger(SubscriberWebSocketEndpoint.class);
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];

    private final String webSocketPath;
    private Session session;

    private final Semaphore semaphore = new Semaphore(0);

    private final Function<Configuration, Flow.Subscriber<Object>> subscriberFactory;
    private String configurationMessage = "";
    private Configuration subscriberConfiguration;
    private Flow.Subscriber<Object> subscriber;

    private final MessageBodyReader<Object> eventReader;
    private final MessageBodyWriter<Object> resultWriter;

    private final ByteBufferAccumulator byteBufferAccumulator;
    private final ByteBufferPool byteBufferPool;

    private Type eventType;
    private final ObjectMapper objectMapper;
    private Marker marker;

    public SubscriberWebSocketEndpoint(String webSocketPath,
                                       Function<Configuration, Flow.Subscriber<Object>> subscriberFactory,
                                       MessageBodyWriter<Object> resultWriter,
                                       MessageBodyReader<Object> eventReader,
                                       ObjectMapper objectMapper,
                                       Type eventType,
                                       ByteBufferPool byteBufferPool) {
        this.webSocketPath = webSocketPath;
        this.subscriberFactory = subscriberFactory;
        this.resultWriter = resultWriter;
        this.eventReader = eventReader;
        this.eventType = eventType;
        this.objectMapper = objectMapper;
        this.byteBufferPool = byteBufferPool;
        this.byteBufferAccumulator = new ByteBufferAccumulator(byteBufferPool, false);
        this.marker = MarkerManager.getMarker(webSocketPath);
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
                    subscriber = subscriberFactory.apply(subscriberConfiguration);
                    subscriber.onSubscribe(new WebSocketSubscription(session, marker));

                    logger.info(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());

                } catch (JsonProcessingException e) {
                    logger.error("Could not parse subscriber configuration", e);
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
/* Move this to reader that can delegate
            JsonFactory jf = new JsonFactory(objectMapper);
            JsonParser jp = jf.createParser(inputStream);
            JsonToken metadataToken = jp.nextToken();
            Metadata metadata = jp.readValueAs(Metadata.class);
            long location = jp.getCurrentLocation().getByteOffset();
            byteBuffer.position((int) location);
*/
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom((Class<Object>) eventType, eventType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, null, inputStream);
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);

            if (resultWriter != null) {
                CompletableFuture<?> resultFuture = new CompletableFuture<>();
                resultFuture.whenComplete(this::sendResult);
                event = new WithResult<>(event, resultFuture);
            }

            subscriber.onNext(event);
        } catch (IOException e) {
            logger.error("Could not receive value", e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
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
                resultWriter.writeTo(result, null, null, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, null, resultOutputStream);
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
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
        } else {
            logger.warn(marker, "Subscriber websocket error", cause);
            if (subscriber != null) {
                subscriber.onError(cause);
            }
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.info(marker, "Complete subscription to {}:{} {}", webSocketPath, statusCode, reason);
        try {
            if (subscriber != null)
                subscriber.onComplete();
        } catch (Exception e) {
            logger.warn(marker, "Could not close subscription sink", e);
        }
    }
}
