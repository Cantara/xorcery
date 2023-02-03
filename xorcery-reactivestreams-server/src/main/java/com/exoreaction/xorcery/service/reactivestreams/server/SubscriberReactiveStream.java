package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.util.ByteBufferBackedInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferAccumulator;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class SubscriberReactiveStream
        extends ServerReactiveStream
        implements WebSocketPartialListener,
        WebSocketConnectionListener,
        Flow.Subscription {
    private final static Logger logger = LogManager.getLogger(SubscriberReactiveStream.class);

    private final Semaphore semaphore = new Semaphore(0);

    private final Function<Configuration, Flow.Subscriber<Object>> subscriberFactory;
    private String configurationMessage = "";
    private Configuration subscriberConfiguration;
    protected Flow.Subscriber<Object> subscriber;

    protected final MessageReader<Object> eventReader;

    protected final ByteBufferAccumulator byteBufferAccumulator;
    protected final ByteBufferPool byteBufferPool;

    protected final ObjectMapper objectMapper;
    protected final Marker marker;
    protected final Executor executor;

    protected Session session;

    // Subscription
    private final AtomicLong requests = new AtomicLong();
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public SubscriberReactiveStream(String streamName,
                                    Function<Configuration, Flow.Subscriber<Object>> subscriberFactory,
                                    MessageReader<Object> eventReader,
                                    ObjectMapper objectMapper,
                                    ByteBufferPool byteBufferPool,
                                    Executor executor) {
        this.subscriberFactory = subscriberFactory;
        this.eventReader = eventReader;
        this.objectMapper = objectMapper;
        this.byteBufferPool = byteBufferPool;
        this.byteBufferAccumulator = new ByteBufferAccumulator(byteBufferPool, false);
        this.marker = MarkerManager.getMarker(streamName);
        this.executor = executor;
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
            executor.execute(this::sendRequests);
    }

    @Override
    public void cancel() {
        logger.trace(marker, "cancel() called");
        cancelled.set(true);
        requests.set(Long.MIN_VALUE);
        if (session != null)
            executor.execute(this::sendRequests);
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


    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public void onWebSocketPartialText(String message, boolean fin) {
        if (subscriberConfiguration == null) {
            configurationMessage += message;

            if (fin) {
                // Read JSON parameters
                try {
                    ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(configurationMessage);
                    subscriberConfiguration = new Configuration.Builder(configurationJson)
                            .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                            .build();
                } catch (JsonProcessingException e) {
                    logger.error("Could not parse subscriber configuration", e);
                    session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
                }

                try {
                    subscriber = subscriberFactory.apply(subscriberConfiguration);
                    subscriber.onSubscribe(this);

                    logger.info(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());
                } catch (Throwable e) {
                    // TODO Send exception here
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

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            logger.debug(marker, "Received:" + Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);
            subscriber.onNext(event);
        } catch (Throwable e) {
            logger.error("Could not receive value", e);
            subscriber.onError(e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
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
        logger.info(marker, "Complete subscription:{} {}", statusCode, reason);
        try {
            if (subscriber != null) subscriber.onComplete();
        } catch (Exception e) {
            logger.warn(marker, "Could not close subscription sink", e);
        }
    }
}
