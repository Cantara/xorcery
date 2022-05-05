package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherWebSocketEndpoint<T>
        implements WebSocketListener, ReactiveEventStreams.Subscriber<T> {
    private final static Logger logger = LogManager.getLogger(PublisherWebSocketEndpoint.class);
    private Session session;
    private Semaphore semaphore = new Semaphore(0);
    private ReactiveEventStreams.Publisher<T> publisher;
    private Map<String, String> parameters;
    private MessageBodyWriter<T> messageBodyWriter;
    private MessageBodyReader<?> messageBodyReader;
    private ReactiveEventStreams.Subscription subscription;

    private Type resultType;
    private final ObjectMapper objectMapper;

    private final AtomicReference<CompletableFuture<Object>> resultFuture = new AtomicReference<>();
    private Disruptor<Event<T>> disruptor;

    public PublisherWebSocketEndpoint(ReactiveEventStreams.Publisher<T> publisher,
                                      Map<String, String> parameters,
                                      MessageBodyWriter<T> messageBodyWriter,
                                      MessageBodyReader<?> messageBodyReader,
                                      Type resultType,
                                      ObjectMapper objectMapper) {
        this.publisher = publisher;
        this.parameters = parameters;
        this.messageBodyWriter = messageBodyWriter;
        this.messageBodyReader = messageBodyReader;
        this.resultType = resultType;
        this.objectMapper = objectMapper;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    // WebSocket
    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);
            Object result = messageBodyReader.readFrom((Class)resultType, resultType, new Annotation[0], MediaType.APPLICATION_OCTET_STREAM_TYPE,
                    new MultivaluedHashMap<String, String>(), bin);

            resultFuture.get().complete((Object) result);
        } catch (Throwable e) {
            logger.error("Could not read result", e);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        long requestAmount = Long.parseLong(message);
        semaphore.release((int) requestAmount);
        subscription.request(requestAmount);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        subscription.cancel();
    }

    @Override
    public void onWebSocketConnect(Session session) {
        this.session = session;
        publisher.subscribe(this, parameters);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
    }

    // Subscriber
    @Override
    public EventSink<Event<T>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        this.subscription = subscription;

        disruptor = new Disruptor<>(Event::new, 4096, new NamedThreadFactory("WebSocketDisruptor-"));

        if (messageBodyReader != null)
            disruptor.handleEventsWith(new Sender()).then(new Receiver());
        else
            disruptor.handleEventsWith(new Sender());

        disruptor.start();
        return disruptor.getRingBuffer();
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error("Reactive socket error", throwable);

    }

    @Override
    public void onComplete() {
        disruptor.shutdown();
        session.close();
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

            ByteBuffer metadataBuffer = ByteBuffer.wrap(objectMapper.writeValueAsBytes(event.metadata));

            session.getRemote().sendBytes(metadataBuffer, new WriteCallback() {
                @Override
                public void writeFailed(Throwable t) {
                    logger.error("Could not send metadata", t);
                    // TODO
                }

                @Override
                public void writeSuccess() {
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    try {
                        if (messageBodyReader == null) {
                            messageBodyWriter.writeTo(event.event, event.event.getClass(), event.event.getClass(), new Annotation[0], null, null, bout);
                        } else {
                            messageBodyWriter.writeTo(((EventWithResult<T, ?>) event.event).event(), event.event.getClass(), event.event.getClass(), new Annotation[0], null, null, bout);
                        }

                        ByteBuffer eventBuffer = ByteBuffer.wrap(bout.toByteArray());

                        session.getRemote().sendBytes(eventBuffer, new WriteCallback() {
                            @Override
                            public void writeFailed(Throwable t) {
                                // TODO
                                logger.error("Could not send event", t);
                            }

                            @Override
                            public void writeSuccess() {
                                // TODO
                            }
                        });
                    } catch (Throwable t) {
                        logger.error("Could not send event", t);
                    }
                }
            });
        }
    }

    public class Receiver
            implements EventHandler<Event<T>> {
        @Override
        public void onEvent(Event<T> event, long sequence, boolean endOfBatch) throws Exception {

            CompletableFuture<Object> result = ((EventWithResult<?, Object>) event.event).result().toCompletableFuture();
            resultFuture.set(result);

            // Wait for result
            result.join();
        }
    }

}
