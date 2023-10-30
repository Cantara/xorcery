/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.client;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.common.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class SubscribeWithResultReactiveStream
        extends SubscribeReactiveStream {

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final MessageWriter<Object> resultWriter;
    boolean isFlushPending = false;
    private ByteBufferOutputStream2 resultOutputStream;

    protected final Meter resultsSent;

    public SubscribeWithResultReactiveStream(URI serverUri,
                                             String streamName,
                                             ClientConfiguration subscriberConfiguration,
                                             DnsLookup dnsLookup,
                                             WebSocketClient webSocketClient,
                                             Subscriber<Object> subscriber,
                                             MessageReader<Object> eventReader,
                                             MessageWriter<Object> resultWriter,
                                             Supplier<Configuration> publisherConfiguration,
                                             ByteBufferPool pool,
                                             MetricRegistry metricRegistry,
                                             Logger logger,
                                             ActiveSubscriptions activeSubscriptions,
                                             CompletableFuture<Void> result) {
        super(serverUri, streamName, subscriberConfiguration, dnsLookup, webSocketClient, subscriber, eventReader, publisherConfiguration, pool, metricRegistry, logger, activeSubscriptions, result);

        this.resultsSent = metricRegistry.meter("subscribe." + streamName + ".results");

        this.resultWriter = resultWriter;
        resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketBinary {}", Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()).toString());
        }
        try {
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            receivedBytes.update(byteBuffer.position());
            byteBufferAccumulator.getByteBufferPool().release(byteBuffer);

            CompletableFuture<Object> resultFuture = new CompletableFuture<>();
            resultFuture.whenComplete(this::sendResult);
            resultQueue.add(resultFuture);
            event = new WithResult<>(event, resultFuture);

            subscriber.onNext(event);
        } catch (IOException e) {
            logger.error(marker, "Could not receive value", e);
            subscriber.onError(e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            result.completeExceptionally(e);
        }
    }

    private synchronized void sendResult(Object result, Throwable throwable) {
        // Send result back, but from the queue so that we ensure ordering
        CompletableFuture<Object> future;
        isFlushPending = false;
        while ((future = resultQueue.peek()) != null && future.isDone()) {
            resultQueue.remove();

            future.whenComplete((r, t) ->
            {
                try {
                    if (t != null) {
                        resultOutputStream.write(ReactiveStreamsAbstractService.XOR);
                        ObjectOutputStream out = new ExceptionObjectOutputStream(resultOutputStream);
                        out.writeObject(t);
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
            });
        }
    }
}
