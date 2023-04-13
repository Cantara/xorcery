package com.exoreaction.xorcery.service.reactivestreams.client;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class PublishWithResultReactiveStream
        extends PublishReactiveStream {
    private final static Logger logger = LogManager.getLogger(PublishReactiveStream.class);
    private final MessageReader<Object> resultReader;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final Meter received;

    public PublishWithResultReactiveStream(String defaultScheme,
                                           String authorityOrBaseUri,
                                           String streamName,
                                           Configuration publisherConfiguration,
                                           DnsLookup dnsLookup,
                                           WebSocketClient webSocketClient,
                                           Flow.Publisher<Object> publisher,
                                           MessageWriter<Object> eventWriter,
                                           MessageReader<Object> resultReader,
                                           Supplier<Configuration> subscriberConfiguration,
                                           ScheduledExecutorService timer,
                                           ByteBufferPool pool,
                                           MetricRegistry metricRegistry,
                                           CompletableFuture<Void> result) {
        super(defaultScheme, authorityOrBaseUri, streamName, publisherConfiguration, dnsLookup, webSocketClient, publisher, eventWriter, subscriberConfiguration, timer, pool, metricRegistry, result);
        this.resultReader = resultReader;
        this.received = metricRegistry.meter("publish." + streamName + ".received");

    }

    public void onComplete() {
        CompletableFuture.runAsync(() ->
        {
            logger.info(marker, "Waiting for outstanding events to be sent to {}", session.getRemote().getRemoteAddress());
            disruptor.shutdown();
            if (!resultQueue.isEmpty()) {
                logger.info(marker, "Waiting for outstanding results to be received from {}", session.getRemote().getRemoteAddress());
                while (!resultQueue.isEmpty()) {
                    // Wait for results to finish
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
            result.complete(null);
            logger.info(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
            session.close(1000, "complete");
        });
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        try {
            // Check if we are getting an exception back
            if (len > ReactiveStreamsAbstractService.XOR.length && Arrays.equals(payload, offset, offset + ReactiveStreamsAbstractService.XOR.length, ReactiveStreamsAbstractService.XOR, 0, ReactiveStreamsAbstractService.XOR.length)) {
                ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset + ReactiveStreamsAbstractService.XOR.length, len - ReactiveStreamsAbstractService.XOR.length);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Throwable throwable = (Throwable) oin.readObject();
                resultQueue.remove().completeExceptionally(throwable);
            } else {
                // Deserialize result
                ByteArrayInputStream bin = new ByteArrayInputStream(payload, offset, len);

                Object result = resultReader.readFrom(bin);

                logger.trace(marker, "Deserialized result: {}", result);
                received.mark();
                resultQueue.remove().complete(result);
            }
        } catch (Throwable e) {
            logger.error(marker, "Could not read result", e);
            resultQueue.remove().completeExceptionally(e);
        }
    }

    @Override
    public void onEvent(AtomicReference<Object> event, long sequence, boolean endOfBatch) throws Exception {
        try {
            ByteBufferOutputStream2 outputStream = new ByteBufferOutputStream2(pool, true);

            // Write event data
            try {
                Object item = event.get();
                WithResult<?, Object> withResult = (WithResult<?, Object>) item;
                CompletableFuture<Object> result = withResult.result().toCompletableFuture();
                resultQueue.add(result);

                eventWriter.writeTo(withResult.event(), outputStream);
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

            sent.mark();
            if (endOfBatch) {
                while (!semaphore.tryAcquire((int) batchSize, 1, TimeUnit.SECONDS) && !result.isDone()) {
                    if (session == null || !session.isOpen())
                        return;
                }

                session.getRemote().flush();
                sentBatchSize.update(batchSize);
            }
        } catch (Throwable e) {
            if (session != null)
                throw new RuntimeException(e);
        }
    }

}
