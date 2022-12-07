package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherReactiveStream
        implements WebSocketListener, Flow.Subscriber<Object> {
    private final static Logger logger = LogManager.getLogger(PublisherReactiveStream.class);

    private final String streamName;
    private Session session;

    private final Function<Configuration, Flow.Publisher<Object>> publisherFactory;
    private Configuration publisherConfiguration;
    private Flow.Publisher<Object> publisher;
    private Flow.Subscription subscription;
    private final Semaphore requestSemaphore = new Semaphore(0);

    private final MessageWriter<Object> messageWriter;
    private final MessageReader<Object> messageReader;

    private final ObjectMapper objectMapper;
    private final ByteBufferPool pool;
    private final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private Disruptor<AtomicReference<Object>> disruptor;
    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private long outstandingRequestAmount;

    public PublisherReactiveStream(String streamName,
                                   Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                   MessageWriter<Object> messageWriter,
                                   MessageReader<Object> messageReader,
                                   ObjectMapper objectMapper,
                                   ByteBufferPool pool) {
        this.streamName = streamName;
        this.publisherFactory = publisherFactory;
        this.messageWriter = messageWriter;
        this.messageReader = messageReader;
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(streamName);
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public void onWebSocketText(String message) {

        if (publisherConfiguration == null) {
            // Read JSON parameters
            try {
                publisherConfiguration = new Configuration((ObjectNode) objectMapper.readTree(message));
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
        try {
            if (messageReader != null) {
                // Check if we are getting an exception back
                // TODO Change this to not require JSON Object as result. Mark exceptions with magic string instead
                if (((char) payload[0]) != '{') {
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    Throwable throwable = (Throwable) oin.readObject();
                    resultQueue.remove().completeExceptionally(throwable);
                } else {
                    // Deserialize result
                    Object result = messageReader.readFrom(new ByteArrayInputStream(payload, offset, len));
                    resultQueue.remove().complete(result);
                }
            } else {
                if (!redundancyNotificationIssued) {
                    logger.warn(marker, "Receiving redundant results from subscriber");
                    redundancyNotificationIssued = true;
                }
            }
        } catch (Throwable e) {
            logger.error(marker, "Could not read result", e);
            resultQueue.remove().completeExceptionally(e);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (statusCode != 1006 && (reason == null || !reason.equals("complete")))
            subscription.cancel();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
            subscription.cancel();
        } else {
            logger.error(marker, "Publisher websocket error", cause);
        }
    }

    // Subscriber

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;

        disruptor = new Disruptor<>(AtomicReference::new, 512, new NamedThreadFactory("PublisherWebSocketDisruptor-" + marker.getName() + "-"));
        disruptor.handleEventsWith(new Sender());
        disruptor.start();

        if (outstandingRequestAmount > 0) {
            subscription.request(outstandingRequestAmount);
            outstandingRequestAmount = 0;
        }
    }

    @Override
    public void onNext(Object item) {
        try {
            while (session.isOpen() && !requestSemaphore.tryAcquire(1, TimeUnit.SECONDS)) {
            }

            if (!session.isOpen())
                return;

            disruptor.publishEvent((ref, seq, e) -> {
                ref.set(e);
            }, item);

        } catch (InterruptedException e) {
            // Ignore
        }
    }

    public void onError(Throwable throwable) {
        subscription = null;

        logger.error(marker, "Reactive publisher error", throwable);
        session.close(StatusCode.SERVER_ERROR, throwable.getMessage());
    }

    public void onComplete() {
        subscription = null;

        logger.info(marker, "Sending complete for session with {}", session.getRemote().getRemoteAddress());
        disruptor.shutdown();

        // TODO This should wait for results to come in
        session.close(1000, "complete");
    }

    // EventHandler
    public class Sender
            implements EventHandler<AtomicReference<Object>> {

        @Override
        public void onEvent(AtomicReference<Object> event, long sequence, boolean endOfBatch) throws Exception {

            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write event data
            try {
                Object item = event.get();
                if (messageWriter != null) {
                    if (messageReader == null) {
                        messageWriter.writeTo(item, outputStream);
                    } else {
                        WithResult<?, Object> withResult = (WithResult<?, Object>) item;
                        CompletableFuture<Object> result = withResult.result().toCompletableFuture();
                        resultQueue.add(result);
                        messageWriter.writeTo(withResult.event(), outputStream);
                    }
                } else {
                    ByteBuffer eventBuffer = (ByteBuffer) event.get();
                    outputStream.write(eventBuffer);
                }
            } catch (Throwable t) {
                logger.error(marker, "Could not send event", t);
                subscription.cancel();
            }

            // Send it
            ByteBuffer eventBuffer = outputStream.takeByteBuffer();

            session.getRemote().sendBytes(eventBuffer, new WriteCallback() {
                @Override
                public void writeFailed(Throwable t) {
                    pool.release(eventBuffer);
                    onWebSocketError(t);
                }

                @Override
                public void writeSuccess() {
                    pool.release(eventBuffer);
                }
            });

            if (endOfBatch)
                session.getRemote().flush();
        }
    }
}
