package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherWebSocketEndpoint<T>
        implements WebSocketListener, ReactiveEventStreams.Subscriber<T> {
    private final static Logger logger = LogManager.getLogger(PublisherWebSocketEndpoint.class);
    private Session session;
    private Semaphore semaphore = new Semaphore(0);
    private String webSocketPath;
    private ReactiveEventStreams.Publisher<T> publisher;
    private Configuration publisherConfiguration;
    private MessageBodyWriter<T> messageBodyWriter;
    private MessageBodyReader<?> messageBodyReader;
    private ReactiveEventStreams.Subscription subscription;

    private Type resultType;
    private final ObjectMapper objectMapper;
    private ByteBufferPool pool;
    private Marker marker;
    private boolean redundancyNotificationIssued = false;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private Disruptor<Event<T>> disruptor;

    public PublisherWebSocketEndpoint(String webSocketPath, ReactiveEventStreams.Publisher<T> publisher,
                                      MessageBodyWriter<T> messageBodyWriter,
                                      MessageBodyReader<?> messageBodyReader,
                                      Type resultType,
                                      ObjectMapper objectMapper,
                                      ByteBufferPool pool,
                                      Marker marker) {
        this.webSocketPath = webSocketPath;
        this.publisher = publisher;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.resultType = resultType;
        this.objectMapper = objectMapper;
        this.pool = pool;
        this.marker = marker;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public synchronized void onWebSocketText(String message) {

        if (publisherConfiguration == null)
        {
            // Read JSON parameters
            try {
                publisherConfiguration = new Configuration((ObjectNode) objectMapper.readTree(message));
                publisher.subscribe(this, publisherConfiguration);
            } catch (JsonProcessingException e) {
                session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            }
        } else
        {
            long requestAmount = Long.parseLong(message);

            if (requestAmount == Long.MIN_VALUE)
            {
                logger.info(marker, "Received cancel on websocket "+webSocketPath);
                subscription.cancel();
            } else {
                if (logger.isDebugEnabled())
                    logger.debug(marker, "Received request:"+requestAmount);

                semaphore.release((int) requestAmount);
                subscription.request(requestAmount);
            }
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            if (messageBodyReader != null)
            {
                // Check if we are getting an exception back
                if (((char)payload[0]) != '{')
                {
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
                    ObjectInputStream oin = new ObjectInputStream(bin);
                    Throwable throwable = (Throwable) oin.readObject();
                    resultQueue.remove().completeExceptionally(throwable);
                } else
                {
                    // Deserialize result
                    ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
                    Object result = messageBodyReader.readFrom((Class)resultType, resultType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE,
                            new MultivaluedHashMap<String, String>(), bin);

                    resultQueue.remove().complete(result);
                }
            } else
            {
                if (!redundancyNotificationIssued)
                {
                    logger.warn(marker, "Receiving redundant results from subscriber");
                    redundancyNotificationIssued = true;
                }
            }
        } catch (Throwable e) {
            logger.error(marker,"Could not read result", e);
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
        if (cause instanceof ClosedChannelException)
        {
            // Ignore
            subscription.cancel();
        } else {
            logger.error(marker,"Publisher websocket error", cause);
        }
    }

    // Subscriber
    @Override
    public EventSink<Event<T>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        this.subscription = subscription;

        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("PublisherWebSocketDisruptor-"+marker.getName()+"-"));

        if (messageBodyReader != null)
            disruptor.handleEventsWith(new Sender()).then(new Receiver());
        else
            disruptor.handleEventsWith(new Sender());

        disruptor.start();
        return disruptor.getRingBuffer();
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error(marker,"Reactive socket error", throwable);
        // Send this to subscriber websocket ?
    }

    @Override
    public void onComplete() {
        logger.info(marker,"Sending complete on websocket {} for session {}",webSocketPath, session.getRemote().getRemoteAddress());
        disruptor.shutdown();
        session.close(1000, "complete");
    }

    // EventHandler
    public class Sender
            implements EventHandler<Event<T>> {

        @Override
        public void onEvent(Event<T> event, long sequence, boolean endOfBatch) throws Exception {
            while (!semaphore.tryAcquire(1, TimeUnit.SECONDS)) {
                if (!session.isOpen())
                    return;
            }

            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write metadata
            objectMapper.writeValue(outputStream, event.metadata);

            // Write event data
            try {
                if (messageBodyWriter != null)
                {
                    if (messageBodyReader == null) {
                        messageBodyWriter.writeTo(event.event, event.event.getClass(), event.event.getClass(), new Annotation[0], null, null, outputStream);
                    } else {

                        EventWithResult<?, Object> eventWithResult = (EventWithResult<?, Object>) event.event;
                        CompletableFuture<Object> result = eventWithResult.result().toCompletableFuture();
                        resultQueue.add(result);

                        messageBodyWriter.writeTo((T)eventWithResult.event(), event.event.getClass(), event.event.getClass(), new Annotation[0], null, null, outputStream);
                    }
                } else {
                    ByteBuffer eventBuffer = (ByteBuffer) event.event;
                    outputStream.writeTo(eventBuffer);
                }
            } catch (Throwable t) {
                logger.error(marker,"Could not send event", t);
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

    public class Receiver
            implements EventHandler<Event<T>> {
        @Override
        public void onEvent(Event<T> event, long sequence, boolean endOfBatch) throws Exception {

        }
    }
}
