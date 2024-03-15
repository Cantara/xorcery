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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.io.ByteBufferBackedInputStream;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.util.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferAccumulator;
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
    private final ByteBufferPool byteBufferPool;
    private ByteBufferAccumulator byteBufferAccumulator;
    private ByteBufferOutputStream2 resultOutputStream;

    protected final LongCounter resultsSent;

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
                                             OpenTelemetry openTelemetry,
                                             Logger logger,
                                             ActiveSubscriptions activeSubscriptions,
                                             CompletableFuture<Void> result) {
        super(serverUri, streamName, subscriberConfiguration, dnsLookup, webSocketClient, subscriber, eventReader, publisherConfiguration, openTelemetry, logger, activeSubscriptions, result);

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        this.resultsSent = meter.counterBuilder("reactivestream.subscribe.results").setUnit("{item}").build();

        this.resultWriter = resultWriter;
        this.byteBufferPool = pool;
        this.byteBufferAccumulator = new ByteBufferAccumulator(pool, false);
        this.resultOutputStream = new ByteBufferOutputStream2(pool, true);
    }

    protected void onWebSocketBinary(ByteBuffer byteBuffer) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketBinary {}", Charset.defaultCharset().decode(byteBuffer.asReadOnlyBuffer()).toString());
        }
        try {
            ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
            Object event = eventReader.readFrom(inputStream);
            receivedBytes.record(byteBuffer.position(), attributes);
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
