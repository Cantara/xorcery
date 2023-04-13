package com.exoreaction.xorcery.service.reactivestreams.server;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.service.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.util.ByteBufferBackedInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.function.Function;

public class SubscriberWithResultReactiveStream
        extends SubscriberReactiveStream {
    private final static Logger logger = LogManager.getLogger(SubscriberReactiveStream.class);


    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final MessageWriter<Object> resultWriter;

    protected final Meter resultsSent;

    public SubscriberWithResultReactiveStream(String streamName,
                                              Function<Configuration, Flow.Subscriber<Object>> subscriberFactory,
                                              MessageReader<Object> eventReader,
                                              MessageWriter<Object> resultWriter,
                                              ObjectMapper objectMapper,
                                              ByteBufferPool byteBufferPool,
                                              Executor executor, MetricRegistry metricRegistry) {
        super(streamName, subscriberFactory, eventReader, objectMapper, byteBufferPool, executor, metricRegistry);

        this.resultsSent = metricRegistry.meter("subscriber." + streamName + ".results");

        this.resultWriter = resultWriter;
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            if (logger.isDebugEnabled())
            {
                logger.debug(marker, "Received:" + Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));
            }
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            received.mark();
            receivedBytes.mark(byteBuffer.position());
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);

            CompletableFuture<Object> resultFuture = new CompletableFuture<>();
            resultFuture.whenComplete(this::sendResult);
            resultQueue.add(resultFuture);
            event = new WithResult<>(event, resultFuture);

            subscriber.onNext(event);
        } catch (Throwable e) {
            logger.error("Could not receive value", e);
            subscriber.onError(e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
        }
    }

    private synchronized void sendResult(Object result, Throwable throwable) {
        // Send result back, but from the queue so that we ensure ordering
        // TODO do something more clever than synchronized

        CompletableFuture<Object> future;
        while ((future = resultQueue.peek()) != null && future.isDone()) {
            resultQueue.remove();

            future.handle((r, t) ->
            {
                ByteBufferOutputStream2 resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);
                try {
                    if (t != null) {
                        resultOutputStream.write(ReactiveStreamsAbstractService.XOR);
                        ObjectOutputStream out = new ExceptionObjectOutputStream(resultOutputStream);
                        out.writeObject(throwable);
                    } else {
                        resultWriter.writeTo(r, resultOutputStream);
                    }

                    ByteBuffer data = resultOutputStream.takeByteBuffer();
                    session.getRemote().sendBytes(data, new WriteCallback() {
                        @Override
                        public void writeFailed(Throwable x) {
                            logger.error(marker, "Could not send result", x);
                            byteBufferPool.release(data);
                        }

                        @Override
                        public void writeSuccess() {
                            byteBufferPool.release(data);
                            resultsSent.mark();
                            logger.trace("Sent result: {}", r);
                        }
                    });
                    session.getRemote().flush();
                } catch (IOException ex) {
                    logger.error(marker, "Could not send result", ex);
                    session.close(StatusCode.SERVER_ERROR, ex.getMessage());
                }
                return null;
            });
        }

    }
}
