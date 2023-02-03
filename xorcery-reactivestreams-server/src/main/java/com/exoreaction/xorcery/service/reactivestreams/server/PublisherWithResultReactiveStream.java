package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class PublisherWithResultReactiveStream
        extends PublisherReactiveStream {
    private final static Logger logger = LogManager.getLogger(PublisherReactiveStream.class);

    private final MessageReader<Object> resultReader;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();

    public PublisherWithResultReactiveStream(String streamName,
                                             Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                             MessageWriter<Object> messageWriter,
                                             MessageReader<Object> resultReader,
                                             ObjectMapper objectMapper,
                                             ByteBufferPool pool) {
        super(streamName, publisherFactory, messageWriter, objectMapper, pool);
        this.resultReader = resultReader;
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
                Object result = resultReader.readFrom(new ByteArrayInputStream(payload, offset, len));
                resultQueue.remove().complete(result);
            }
        } catch (Throwable e) {
            logger.error(marker, "Could not read result", e);
            resultQueue.remove().completeExceptionally(e);
        }
    }

    @Override
    protected EventHandler<AtomicReference<Object>> createSender() {
        return new SenderWithResult();
    }

    // EventHandler
    public class SenderWithResult
            extends Sender {
        @Override
        protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
            WithResult<?, Object> withResult = (WithResult<?, Object>) item;
            CompletableFuture<Object> result = withResult.result().toCompletableFuture();
            resultQueue.add(result);
            messageWriter.writeTo(withResult.event(), outputStream);
        }
    }
}
