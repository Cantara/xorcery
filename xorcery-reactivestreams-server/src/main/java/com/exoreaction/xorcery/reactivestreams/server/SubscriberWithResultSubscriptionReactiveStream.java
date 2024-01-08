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
package com.exoreaction.xorcery.reactivestreams.server;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.common.ActiveSubscriptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class SubscriberWithResultSubscriptionReactiveStream
        extends SubscriberSubscriptionReactiveStream {
    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final MessageWriter<Object> resultWriter;
    boolean isFlushPending = false;
    private final ByteBufferOutputStream2 resultOutputStream;

    protected final Meter resultsSent;

    public SubscriberWithResultSubscriptionReactiveStream(String streamName,
                                                          Function<Configuration, Subscriber<Object>> subscriberFactory,
                                                          MessageReader<Object> eventReader,
                                                          MessageWriter<Object> resultWriter,
                                                          ObjectMapper objectMapper,
                                                          ByteBufferPool byteBufferPool,
                                                          MetricRegistry metricRegistry,
                                                          Logger logger,
                                                          ActiveSubscriptions activeSubscriptions) {
        super(streamName, subscriberFactory, eventReader, objectMapper, byteBufferPool, metricRegistry, logger, activeSubscriptions);

        this.resultsSent = metricRegistry.meter("subscriber." + streamName + ".results");

        this.resultWriter = resultWriter;
        resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()).toString());
            }

            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            receivedBytes.update(byteBuffer.position());
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
                        resultWriter.writeTo(r, resultOutputStream);
                    }

                    ByteBuffer data = resultOutputStream.takeByteBuffer();
                    session.getRemote().sendBytes(data, new WriteCallback() {
                        @Override
                        public void writeFailed(Throwable t) {
                            byteBufferPool.release(data);

                            if (t instanceof ClosedChannelException)
                                return;

                            logger.error(marker, "Could not send result", t);
                        }

                        @Override
                        public void writeSuccess() {
                            byteBufferPool.release(data);
                            resultsSent.mark();
                            logger.trace("Sent result: {}", r);
                            isFlushPending = true;
                        }
                    });
                } catch (IOException ex) {
                    logger.error(marker, "Could not send result", ex);
                    session.close(StatusCode.SERVER_ERROR, ex.getMessage());
                }
            });
        }
        if (isFlushPending) {
            try {
                session.getRemote().flush();
            } catch (IOException e) {
                logger.error(marker, "Could not flush results", e);
                session.close(StatusCode.SERVER_ERROR, e.getMessage());
            }
        }
    }
}
