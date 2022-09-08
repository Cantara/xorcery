package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.util.Classes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.*;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherWebSocketEndpoint
        implements WebSocketListener, Flow.Subscriber<Object> {
    private final static Logger logger = LogManager.getLogger(PublisherWebSocketEndpoint.class);
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private static final MultivaluedHashMap<String, String> EMPTY_HTTP_HEADERS = new MultivaluedHashMap<>();
    private static final MultivaluedHashMap<String, Object> EMPTY_HTTP_HEADERS2 = new MultivaluedHashMap<>();

    private final String webSocketPath;
    private Session session;

    private final Function<Configuration, Flow.Publisher<Object>> publisherFactory;
    private Configuration publisherConfiguration;
    private Flow.Publisher<Object> publisher;
    private Flow.Subscription subscription;
    private final Semaphore requestSemaphore = new Semaphore(0);

    private final Type eventType;
    private final Class<Object> eventClass;
    private final Type resultType;
    private final Class<Object> resultClass;
    private final MessageBodyWriter<Object> messageBodyWriter;
    private final MessageBodyReader<Object> messageBodyReader;

    private final ObjectMapper objectMapper;
    private final ByteBufferPool pool;
    private final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private Disruptor<AtomicReference<Object>> disruptor;
    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();

    public PublisherWebSocketEndpoint(String webSocketPath, Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                      MessageBodyWriter<Object> messageBodyWriter,
                                      MessageBodyReader<Object> messageBodyReader,
                                      Type eventType,
                                      Type resultType,
                                      ObjectMapper objectMapper,
                                      ByteBufferPool pool) {
        this.webSocketPath = webSocketPath;
        this.publisherFactory = publisherFactory;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.eventType = eventType;
        this.eventClass = Classes.getClass(eventType);
        this.resultType = resultType;
        this.resultClass = Classes.getClass(resultType);
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(webSocketPath);
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public synchronized void onWebSocketText(String message) {

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

            if (requestAmount == Long.MIN_VALUE) {
                logger.info(marker, "Received cancel on websocket " + webSocketPath);
                subscription.cancel();
            } else {
                if (logger.isDebugEnabled())
                    logger.debug(marker, "Received request:" + requestAmount);

                requestSemaphore.release((int) requestAmount);
                subscription.request(requestAmount);
            }
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            if (messageBodyReader != null) {
                // Check if we are getting an exception back
                // TODO Change this to not require JSON Object as result. Mark exceptions with magic string instead
                if (((char) payload[0]) != '{') {
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    Throwable throwable = (Throwable) oin.readObject();
                    resultQueue.remove().completeExceptionally(throwable);
                } else {
                    // Deserialize result
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
                    Object result = messageBodyReader.readFrom(resultClass, resultType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE,
                            EMPTY_HTTP_HEADERS, bin);

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
        logger.error(marker, "Reactive publisher error", throwable);
        session.close(StatusCode.SERVER_ERROR, throwable.getMessage());
    }

    public void onComplete() {
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
                if (messageBodyWriter != null) {
                    if (messageBodyReader == null) {
                        messageBodyWriter.writeTo(item, eventClass, eventType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS2, outputStream);
                    } else {
                        WithResult<?, Object> withResult = (WithResult<?, Object>) item;
                        CompletableFuture<Object> result = withResult.result().toCompletableFuture();
                        resultQueue.add(result);

                        messageBodyWriter.writeTo(withResult.event(), eventClass, eventType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS2, outputStream);
                    }
                } else {
                    ByteBuffer eventBuffer = (ByteBuffer) event.get();
                    outputStream.writeTo(eventBuffer);
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
