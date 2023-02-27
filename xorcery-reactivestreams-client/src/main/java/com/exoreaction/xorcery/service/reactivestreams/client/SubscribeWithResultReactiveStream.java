package com.exoreaction.xorcery.service.reactivestreams.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.util.ByteBufferBackedInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public class SubscribeWithResultReactiveStream
        extends SubscribeReactiveStream {

    private static final Logger logger = LogManager.getLogger(SubscribeReactiveStream.class);

    private final MessageWriter<Object> resultWriter;

    public SubscribeWithResultReactiveStream(String defaultScheme,
                                             String authorityOrBaseUri,
                                             String streamName,
                                             Configuration subscriberConfiguration,
                                             DnsLookup dnsLookup,
                                             WebSocketClient webSocketClient,
                                             Flow.Subscriber<Object> subscriber,
                                             MessageReader<Object> eventReader,
                                             MessageWriter<Object> resultWriter,
                                             Supplier<Configuration> publisherConfiguration,
                                             ScheduledExecutorService timer,
                                             ByteBufferPool pool,
                                             CompletableFuture<Void> result) {
        super(defaultScheme, authorityOrBaseUri, streamName, subscriberConfiguration, dnsLookup, webSocketClient, subscriber, eventReader, publisherConfiguration, timer, pool, result);
        this.resultWriter = resultWriter;
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
//                logger.info(marker, "Received:"+ Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()));
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);

            event = new WithResult<>(event, new CompletableFuture<>().whenComplete(this::sendResult));
            subscriber.onNext(event);
        } catch (IOException e) {
            logger.error(marker, "Could not receive value", e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            result.completeExceptionally(e);
        }
    }

    private void sendResult(Object result, Throwable throwable) {
        // Send result back
        ByteBufferOutputStream2 resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);

        try {
            if (throwable != null) {
                resultOutputStream.write(ReactiveStreamsAbstractService.XOR);
                ObjectOutputStream out = new ExceptionObjectOutputStream(resultOutputStream);
                out.writeObject(throwable);
            } else {
                resultWriter.writeTo(result, resultOutputStream);
            }

            ByteBuffer data = resultOutputStream.takeByteBuffer();
            session.getRemote().sendBytes(data, new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    logger.error(marker, "Could not send result", x);
                }

                @Override
                public void writeSuccess() {
                    byteBufferPool.release(data);
                }
            });
        } catch (IOException ex) {
            logger.error(marker, "Could not send result", ex);
            session.close(StatusCode.SERVER_ERROR, ex.getMessage());
            this.result.completeExceptionally(ex); // TODO This should probably do a retry instead
        }
    }

}
