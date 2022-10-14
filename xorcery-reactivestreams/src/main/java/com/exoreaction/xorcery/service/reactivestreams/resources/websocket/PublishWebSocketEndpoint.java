package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.PublishingProcess;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsService;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.util.Classes;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author rickardoberg
 */
public class PublishWebSocketEndpoint
        implements WebSocketListener, Flow.Subscriber<Object> {
    private final static Logger logger = LogManager.getLogger(PublishWebSocketEndpoint.class);
    private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    private static final MultivaluedHashMap<String, String> EMPTY_HTTP_HEADERS = new MultivaluedHashMap<>();
    private static final MultivaluedHashMap<String, Object> EMPTY_HTTP_HEADERS2 = new MultivaluedHashMap<>();

    private final String webSocketPath;
    private Session session;

    private final Flow.Publisher<Object> publisher;
    private Flow.Subscription subscription;
    private final Semaphore semaphore = new Semaphore(0);

    private final MessageBodyWriter<Object> eventWriter;
    private final MessageBodyReader<Object> resultReader;

    private final Type eventType;
    private final Class<Object> eventClass;
    private final Type resultType;
    private final Class<Object> resultClass;
    private final Configuration subscriberConfiguration;
    private final ObjectMapper objectMapper;
    private final ByteBufferPool pool;
    private final Marker marker;
    private final PublishingProcess publishingProcess;

    private boolean redundancyNotificationIssued = false;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private Disruptor<AtomicReference<Object>> disruptor;

    public PublishWebSocketEndpoint(String webSocketPath,
                                    Flow.Publisher<Object> publisher,
                                    MessageBodyWriter<Object> eventWriter,
                                    MessageBodyReader<Object> resultReader,
                                    Type eventType,
                                    Type resultType,
                                    Configuration subscriberConfiguration,
                                    ObjectMapper objectMapper,
                                    ByteBufferPool pool,
                                    PublishingProcess publishingProcess) {
        this.webSocketPath = webSocketPath;
        this.publisher = publisher;
        this.eventWriter = eventWriter;
        this.resultReader = resultReader;
        this.eventType = eventType;
        this.eventClass = Classes.getClass(eventType);
        this.resultType = resultType;
        this.resultClass = Classes.getClass(resultType);
        this.subscriberConfiguration = subscriberConfiguration;
        this.objectMapper = objectMapper;
        this.pool = pool;
        this.marker = MarkerManager.getMarker(webSocketPath);
        this.publishingProcess = publishingProcess;
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;

        // First send parameters, if available
        String parameterString = subscriberConfiguration.json().toPrettyString();
        session.getRemote().sendString(parameterString, new WriteCallbackCompletableFuture().with(f ->
                f.future().thenAccept(Void ->
                {
                    publisher.subscribe(this);
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
    public void onWebSocketText(String message) {

        long requestAmount = Long.parseLong(message);

        if (requestAmount == Long.MIN_VALUE) {
            logger.info(marker, "Received cancel on websocket " + webSocketPath);
            subscription.cancel();
        } else {
            if (logger.isDebugEnabled())
                logger.debug(marker, "Received request:" + requestAmount);

            semaphore.release((int) requestAmount);
            subscription.request(requestAmount);
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            if (resultReader != null) {
                // Check if we are getting an exception back
                if (len > ReactiveStreamsService.XOR.length && Arrays.equals(payload, offset, offset+ReactiveStreamsService.XOR.length, ReactiveStreamsService.XOR, 0, ReactiveStreamsService.XOR.length)) {
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset+ReactiveStreamsService.XOR.length, len-ReactiveStreamsService.XOR.length);
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    Throwable throwable = (Throwable) oin.readObject();
                    resultQueue.remove().completeExceptionally(throwable);
                } else {
                    // Deserialize result
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);

                    Object result = resultReader.readFrom(resultClass, resultType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE,
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

        if (statusCode == 1000 || statusCode == 1001 || statusCode == 1006) {
            logger.info(marker, "Complete subscription to {}:{} {}", webSocketPath, statusCode, reason);
            subscription.cancel();
            publishingProcess.result().complete(null); // Now considered done
        } else {
            logger.info(marker, "Close websocket {}:{} {}", webSocketPath, statusCode, reason);
            logger.info(marker, "Starting subscription process again");
            publishingProcess.retry();
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (cause instanceof ClosedChannelException) {
            // Ignore
            subscription.cancel();
            publishingProcess.result().completeExceptionally(cause); // Now considered done
        } else {
            logger.error(marker, "Publisher websocket error", cause);
        }
    }

    // Subscriber
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;

        disruptor = new Disruptor<>(AtomicReference::new, 512, new NamedThreadFactory("PublishWebSocketDisruptor-" + marker.getName() + "-"));
        disruptor.handleEventsWith(new Sender());
        disruptor.start();
    }

    @Override
    public void onNext(Object item) {
        disruptor.publishEvent((e,s,event)->
        {
            e.set(event);
        }, item);
    }

    @Override
    public void onError(Throwable throwable) {
        logger.info(marker, "Publisher error for {}", session.getRemote().getRemoteAddress(), throwable);
        disruptor.shutdown();
        logger.info(marker, "Sending close for session {}", session.getRemote().getRemoteAddress());
        session.close(StatusCode.SERVER_ERROR, throwable.getMessage());
    }

    public void onComplete() {
        logger.info(marker, "Waiting for outstanding events to be sent to {}", session.getRemote().getRemoteAddress());
        disruptor.shutdown();
        logger.info(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
        while (!resultQueue.isEmpty()) {
            // Wait for results to finish
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        session.close(1000, "complete");
    }

    // EventHandler
    public class Sender
            implements EventHandler<AtomicReference<Object>> {

        @Override
        public void onEvent(AtomicReference<Object> event, long sequence, boolean endOfBatch) throws Exception {
            while (!semaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                if (!session.isOpen())
                    return;
            }

            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write event data
            try {
                Object item = event.get();
                if (eventWriter != null) {
                    if (resultReader == null) {
                        eventWriter.writeTo(item, eventClass, eventType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS2, outputStream);
                    } else {

                        WithResult<?, Object> withResult = (WithResult<?, Object>) item;
                        CompletableFuture<Object> result = withResult.result().toCompletableFuture();
                        resultQueue.add(result);

                        eventWriter.writeTo(withResult.event(), eventClass, eventType, EMPTY_ANNOTATIONS, MediaType.WILDCARD_TYPE, EMPTY_HTTP_HEADERS2, outputStream);
                    }
                } else {
                    ByteBuffer eventBuffer = (ByteBuffer) item;
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
