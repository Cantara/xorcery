package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jetty.client.WriteCallbackCompletableFuture;
import com.exoreaction.xorcery.service.reactivestreams.SubscriptionProcess;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
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
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class SubscribeWebSocketEndpoint
        implements WebSocketPartialListener, WebSocketConnectionListener {

    private static final Logger logger = LogManager.getLogger(SubscribeWebSocketEndpoint.class);
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private static final MultivaluedHashMap<String, String> EMPTY_HTTP_HEADERS = new MultivaluedHashMap<>();
    private static final MultivaluedHashMap<String, Object> EMPTY_HTTP_HEADERS2 = new MultivaluedHashMap<>();

    private final Flow.Subscriber<Object> subscriber;
    private MessageBodyReader<Object> eventReader;
    private MessageBodyWriter<Object> resultWriter;
    private ObjectMapper objectMapper;
    private Session session;
    private Marker marker;

    private ByteBufferAccumulator byteBufferAccumulator;
    private ByteBufferPool byteBufferPool;
    private SubscriptionProcess subscriptionProcess;
    private String webSocketHref;
    private Configuration publisherConfiguration;

    public SubscribeWebSocketEndpoint(Flow.Subscriber<Object> subscriber,
                                      MessageBodyReader<Object> eventReader,
                                      MessageBodyWriter<Object> resultWriter,
                                      ObjectMapper objectMapper,
                                      ByteBufferPool byteBufferPool,
                                      SubscriptionProcess subscriptionProcess,
                                      String webSocketHref,
                                      Configuration publisherConfiguration) {
        this.marker = MarkerManager.getMarker(webSocketHref);
        this.subscriber = subscriber;
        this.eventReader = eventReader;
        this.resultWriter = resultWriter;
        this.objectMapper = objectMapper;
        this.byteBufferAccumulator = new ByteBufferAccumulator(byteBufferPool, false);
        this.byteBufferPool = byteBufferPool;
        this.subscriptionProcess = subscriptionProcess;
        this.webSocketHref = webSocketHref;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        // First send parameters, if available
        String parameterString = publisherConfiguration.json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAccept(Void ->
                {
                    subscriber.onSubscribe(new WebSocketSubscription(session, marker));
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
/* This should be done by reader itself
            JsonParser jp = jf.createParser(inputStream);
            JsonToken metadataToken = jp.nextToken();
            Metadata metadata = jp.readValueAs(Metadata.class);
            long location = jp.getCurrentLocation().getByteOffset();
            byteBuffer.position((int) location);
*/

            Object event = eventReader.readFrom(null, null, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS, inputStream);
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);

            if (resultWriter != null) {
                event = new WithResult<>(event, new CompletableFuture<>().whenComplete(this::sendResult));
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
                resultWriter.writeTo(result, null, null, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS2, resultOutputStream);
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
            logger.error(marker, "Subscriber websocket error", cause);
            subscriber.onError(cause);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        try {
            subscriber.onComplete();
        } catch (Exception e) {
            logger.warn(marker, "Could not close subscription", e);
        }

        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            logger.info(marker, "Complete subscription to {}:{} {}", webSocketHref, statusCode, reason);
            subscriptionProcess.result().complete(null); // Now considered done
        } else {
            logger.warn(marker, "Close websocket {}:{} {}", webSocketHref, statusCode, reason);
            logger.info(marker, "Starting subscription process again");
            subscriptionProcess.retry();
        }
    }

}
