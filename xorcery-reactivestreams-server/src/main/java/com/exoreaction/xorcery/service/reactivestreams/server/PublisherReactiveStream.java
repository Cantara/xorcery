package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ClientStreamException;
import com.exoreaction.xorcery.service.reactivestreams.api.ServerShutdownStreamException;
import com.exoreaction.xorcery.service.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.ws.rs.ClientErrorException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.message.ReusableMessageFactory;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherReactiveStream
        extends ServerReactiveStream
        implements WebSocketListener, Flow.Subscriber<Object> {
    private final static Logger logger = LogManager.getLogger(PublisherReactiveStream.class);

    private final String streamName;
    protected Session session;

    private final Function<Configuration, Flow.Publisher<Object>> publisherFactory;
    private Configuration publisherConfiguration;
    private Flow.Publisher<Object> publisher;
    protected Flow.Subscription subscription;
    private final Semaphore requestSemaphore = new Semaphore(0);

    protected final MessageWriter<Object> messageWriter;

    private final ObjectMapper objectMapper;
    protected final ByteBufferPool pool;
    protected final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private final Disruptor<AtomicReference<Object>> disruptor;
    private long outstandingRequestAmount;

    public PublisherReactiveStream(String streamName,
                                   Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                   MessageWriter<Object> messageWriter,
                                   ObjectMapper objectMapper,
                                   ByteBufferPool pool) {
        this.streamName = streamName;
        this.publisherFactory = publisherFactory;
        this.messageWriter = messageWriter;
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(streamName);

        disruptor = new Disruptor<>(AtomicReference::new, 512, new NamedThreadFactory("PublisherWebSocketDisruptor-" + marker.getName() + "-"));
        disruptor.handleEventsWith(createSender());
        disruptor.start();
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketText {}", message);

        if (publisherConfiguration == null) {
            // Read JSON parameters
            try {
                ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(message);
                publisherConfiguration = new Configuration.Builder(configurationJson)
                        .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                        .build();
                publisher = publisherFactory.apply(publisherConfiguration);
                publisher.subscribe(this);
            } catch (JsonProcessingException e) {
                session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            }
        } else {
            long requestAmount = Long.parseLong(message);

            if (subscription != null) {
                if (requestAmount == Long.MIN_VALUE) {
                    logger.info(marker, "Received cancel on websocket " + streamName);
                    subscription.cancel();
                    subscription = null;
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug(marker, "Received request:" + requestAmount);
                    requestSemaphore.release((int) requestAmount);
                    subscription.request(requestAmount);
                }
            } else {
                outstandingRequestAmount += requestAmount;
            }
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketBinary");

        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketError", cause);

        if (cause instanceof ClosedChannelException && subscription != null) {
            // Ignore
            subscription.cancel();
            subscription = null;
        } else {
            logger.error(marker, "Publisher websocket error", cause);
        }
    }

    // Subscriber

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onSubscribe");

        this.subscription = subscription;

        if (outstandingRequestAmount > 0) {
            subscription.request(outstandingRequestAmount);
            outstandingRequestAmount = 0;
        }
    }

    protected EventHandler<AtomicReference<Object>> createSender() {
        return new Sender();
    }

    @Override
    public void onNext(Object item) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onNext {}", item.toString());

        try {
            while (session.isOpen() && !requestSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
            }

            if (!session.isOpen() || subscription == null)
                return;

            disruptor.publishEvent((ref, seq, e) -> {
                ref.set(e);
            }, item);

        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public void onError(Throwable throwable) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onError", throwable);

        subscription = null;

        disruptor.shutdown();

        if (throwable instanceof ServerShutdownStreamException) {
            session.close(StatusCode.SHUTDOWN, throwable.getMessage());
        } else {
            // Send exception
            // Client should receive exception and close session
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream out = new ExceptionObjectOutputStream(bout);
                out.writeObject(throwable);
                out.close();
                String base64Throwable = Base64.getEncoder().encodeToString(bout.toByteArray());
                session.getRemote().sendString(base64Throwable);
                session.getRemote().flush();
            } catch (IOException e) {
                logger.error(marker, "Could not send exception", e);
            }
        }
    }

    public void onComplete() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onComplete");

        subscription = null;
        disruptor.shutdown();

        session.close(StatusCode.NORMAL, "complete");
    }

    // EventHandler
    public class Sender
            implements EventHandler<AtomicReference<Object>> {

        private CountDownLatch batchFinished;

        @Override
        public void onBatchStart(long batchSize) {
            batchFinished = new CountDownLatch((int) batchSize);
        }

        @Override
        public void onEvent(AtomicReference<Object> event, long sequence, boolean endOfBatch) throws Exception {

            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write event data
            try {
                Object item = event.get();
                writeItem(messageWriter, item, outputStream);

            } catch (Throwable t) {
                logger.error(marker, "Could not send event", t);
                subscription.cancel();
                subscription = null;
            }

            // Send it
            ByteBuffer eventBuffer = outputStream.takeByteBuffer();

            session.getRemote().sendBytes(eventBuffer, new WriteCallback() {
                @Override
                public void writeFailed(Throwable t) {
                    pool.release(eventBuffer);
                    onWebSocketError(t);
                    batchFinished.countDown();
                }

                @Override
                public void writeSuccess() {
                    pool.release(eventBuffer);
                    batchFinished.countDown();
                }
            });

            if (endOfBatch) {
                session.getRemote().flush();
                batchFinished.await(60, TimeUnit.SECONDS);
            }
        }

        protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
            messageWriter.writeTo(item, outputStream);
        }
    }
}
