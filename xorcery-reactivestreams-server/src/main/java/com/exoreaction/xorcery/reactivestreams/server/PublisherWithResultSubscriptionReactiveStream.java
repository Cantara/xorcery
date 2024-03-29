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
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.OpenTelemetry;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Callback;
import org.reactivestreams.Publisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class PublisherWithResultSubscriptionReactiveStream
        extends PublisherSubscriptionReactiveStream {
    private final MessageReader<Object> resultReader;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();

    public PublisherWithResultSubscriptionReactiveStream(String streamName,
                                                         Function<Configuration, Publisher<Object>> publisherFactory,
                                                         MessageWriter<Object> messageWriter,
                                                         MessageReader<Object> resultReader,
                                                         ObjectMapper objectMapper,
                                                         ByteBufferPool pool,
                                                         Logger logger,
                                                         ActiveSubscriptions activeSubscriptions,
                                                         OpenTelemetry openTelemetry) {
        super(streamName, publisherFactory, messageWriter, objectMapper, pool, logger, activeSubscriptions, openTelemetry);
        this.resultReader = resultReader;
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketBinary");

        try {
            // Check if we are getting an exception back
            if (payload.limit() > ReactiveStreamsAbstractService.XOR.length && Arrays.equals(payload.array(), payload.arrayOffset(), payload.position() + ReactiveStreamsAbstractService.XOR.length, ReactiveStreamsAbstractService.XOR, 0, ReactiveStreamsAbstractService.XOR.length)) {
                ByteArrayInputStream bin = new ByteArrayInputStream(payload.array(), payload.arrayOffset() + ReactiveStreamsAbstractService.XOR.length, payload.limit() - ReactiveStreamsAbstractService.XOR.length);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Throwable throwable = (Throwable) oin.readObject();
                resultQueue.remove().completeExceptionally(throwable);
            } else {
                // Deserialize result
                Object result = resultReader.readFrom(new ByteArrayInputStream(payload.array(), payload.arrayOffset(), payload.limit()));
                resultQueue.remove().complete(result);
            }
            callback.succeed();
        } catch (Throwable e) {
            logger.error(marker, "Could not read result", e);
            resultQueue.remove().completeExceptionally(e);
            callback.fail(e);
        }
    }

    @Override
    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        WithResult<?, Object> withResult = (WithResult<?, Object>) item;
        CompletableFuture<Object> result = withResult.result().toCompletableFuture();
        resultQueue.add(result);
        messageWriter.writeTo(withResult.event(), outputStream);
    }
}