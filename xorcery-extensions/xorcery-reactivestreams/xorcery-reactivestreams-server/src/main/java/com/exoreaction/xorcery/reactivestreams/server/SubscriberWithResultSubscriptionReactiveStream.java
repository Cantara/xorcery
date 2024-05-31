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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.util.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsAbstractService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SchemaUrls;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class SubscriberWithResultSubscriptionReactiveStream
        extends SubscriberSubscriptionReactiveStream {
    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final MessageWriter<Object> resultWriter;
    private final ByteBufferOutputStream2 resultOutputStream;

    protected final LongCounter resultsSent;

    public SubscriberWithResultSubscriptionReactiveStream(String streamName,
                                                          Function<Configuration, Subscriber<Object>> subscriberFactory,
                                                          MessageReader<Object> eventReader,
                                                          MessageWriter<Object> resultWriter,
                                                          ObjectMapper objectMapper,
                                                          ByteBufferPool byteBufferPool,
                                                          OpenTelemetry openTelemetry,
                                                          Logger logger,
                                                          ActiveSubscriptions activeSubscriptions) {
        super(streamName, subscriberFactory, eventReader, objectMapper, byteBufferPool, openTelemetry, logger, activeSubscriptions);

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        this.resultsSent = meter.counterBuilder("reactivestream.subscriber.results")
                .setUnit("{item}").build();

        this.resultWriter = resultWriter;
        resultOutputStream = new ByteBufferOutputStream2(byteBufferPool, true);
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        try {
            if (logger.isTraceEnabled()) {
                logger.trace(marker, "onWebSocketBinary {}", Charset.defaultCharset().decode(payload.asReadOnlyBuffer()).toString());
            }

            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(payload);
            Object event = eventReader.readFrom(inputStream);
            receivedBytes.record(payload.position(), attributes);
            CompletableFuture<Object> resultFuture = new CompletableFuture<>();
            resultFuture.whenComplete(this::sendResult);
            resultQueue.add(resultFuture);
            event = new WithResult<>(event, resultFuture);

            subscriber.onNext(event);
            callback.succeed();
        } catch (Throwable e) {
            logger.error("Could not receive value", e);
            subscriber.onError(e);
            session.close(StatusCode.BAD_PAYLOAD, e.getMessage(), Callback.NOOP);
            callback.fail(e);
        }
    }

    private synchronized void sendResult(Object result, Throwable throwable) {
        // Send result back, but from the queue so that we ensure ordering
        CompletableFuture<Object> future;
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

                    RetainableByteBuffer retainableByteBuffer = resultOutputStream.takeByteBuffer();
                    ByteBuffer data = retainableByteBuffer.getByteBuffer();
                    session.sendBinary(data, new Callback() {
                        @Override
                        public void succeed() {
                            retainableByteBuffer.release();
                            resultsSent.add(1, attributes);
                            logger.trace("Sent result: {}", r);
                        }

                        @Override
                        public void fail(Throwable x) {
                            retainableByteBuffer.release();
                        }
                    });
                } catch (IOException ex) {
                    logger.error(marker, "Could not send result", ex);
                    session.close(StatusCode.SERVER_ERROR, ex.getMessage(), Callback.NOOP);
                }
            });
        }
    }
}
