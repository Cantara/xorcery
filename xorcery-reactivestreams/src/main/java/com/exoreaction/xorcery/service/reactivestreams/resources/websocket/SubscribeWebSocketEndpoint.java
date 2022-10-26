package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsService;
import com.exoreaction.xorcery.service.reactivestreams.SubscriptionProcess;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class SubscribeWebSocketEndpoint
        implements WebSocketPartialListener, WebSocketConnectionListener {

    private static final Logger logger = LogManager.getLogger(SubscribeWebSocketEndpoint.class);

    private final String webSocketHref;
    private final Flow.Subscriber<Object> subscriber;
    private final MessageReader<Object> eventReader;
    private final MessageWriter<Object> resultWriter;

    private final Marker marker;
    private final ByteBufferAccumulator byteBufferAccumulator;
    private final ByteBufferPool byteBufferPool;
    private final SubscriptionProcess subscriptionProcess;
    private final Configuration publisherConfiguration;

    private Session session;

    public SubscribeWebSocketEndpoint(Flow.Subscriber<Object> subscriber,
                                      MessageReader<Object> eventReader,
                                      MessageWriter<Object> resultWriter,
                                      ByteBufferPool byteBufferPool,
                                      SubscriptionProcess subscriptionProcess,
                                      String webSocketHref,
                                      Configuration publisherConfiguration) {
        this.marker = MarkerManager.getMarker(webSocketHref);
        this.subscriber = subscriber;
        this.eventReader = eventReader;
        this.resultWriter = resultWriter;
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
                f.future().thenAcceptAsync(Void ->
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
        }
    }

    private void sendResult(Object result, Throwable throwable) {
        // Send result back
        ByteBufferOutputStream2 resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);

        try {
            if (throwable != null) {
                resultOutputStream.write(ReactiveStreamsService.XOR);
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
