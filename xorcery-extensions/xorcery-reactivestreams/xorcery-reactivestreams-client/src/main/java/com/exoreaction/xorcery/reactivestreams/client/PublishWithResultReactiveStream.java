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
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
import com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class PublishWithResultReactiveStream
        extends PublishReactiveStream {
    private final static Logger logger = LogManager.getLogger(PublishWithResultReactiveStream.class);
    private final MessageReader<Object> resultReader;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final LongCounter received;

    public PublishWithResultReactiveStream(URI serverUri,
                                           String streamName,
                                           ClientConfiguration publisherConfiguration,
                                           DnsLookup dnsLookup,
                                           WebSocketClient webSocketClient,
                                           Publisher<Object> publisher,
                                           MessageWriter<Object> eventWriter,
                                           MessageReader<Object> resultReader,
                                           Supplier<Configuration> subscriberConfiguration,
                                           ByteBufferPool pool,
                                           OpenTelemetry openTelemetry,
                                           ActiveSubscriptions activeSubscriptions,
                                           CompletableFuture<Void> result) {
        super(
                serverUri,
                streamName,
                publisherConfiguration,
                dnsLookup,
                webSocketClient,
                publisher,
                eventWriter,
                subscriberConfiguration,
                pool,
                openTelemetry,
                activeSubscriptions,
                result
        );
        this.resultReader = resultReader;
        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        this.received = meter.counterBuilder("reactivestream.publish.received")
                .setUnit("{item}").build();
    }

    @Override
    public void retry(Throwable cause) {
        if (resultQueue != null)
        {
            for (CompletableFuture<Object> resultFuture : resultQueue) {
                resultFuture.completeExceptionally(cause);
            }
            resultQueue.clear();
        }
        super.retry(cause);
    }

    protected void checkDone() {
        if (isComplete.get() && resultQueue.isEmpty()) {
            if (session != null && session.isOpen()) {
                logger.debug(marker, "Sending complete for session {}", session.getRemoteSocketAddress());
                session.close(StatusCode.NORMAL, "complete", Callback.NOOP);

                if (!result.isDone())
                    result.complete(null);
            }
        }
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketBinary");
        }

        try {
            // Check if we are getting an exception back
            if (payload.limit() > ReactiveStreamsAbstractService.XOR.length && Arrays.equals(payload.array(), payload.arrayOffset(), payload.arrayOffset() + ReactiveStreamsAbstractService.XOR.length, ReactiveStreamsAbstractService.XOR, 0, ReactiveStreamsAbstractService.XOR.length)) {
                ByteArrayInputStream bin = new ByteArrayInputStream(payload.array(), payload.arrayOffset() + ReactiveStreamsAbstractService.XOR.length, payload.limit() - ReactiveStreamsAbstractService.XOR.length);
                ObjectInputStream oin = new ObjectInputStream(bin);
                Throwable throwable = (Throwable) oin.readObject();
                resultQueue.remove().completeExceptionally(throwable);
            } else {
                // Deserialize result
                ByteArrayInputStream bin = new ByteArrayInputStream(payload.array(), payload.arrayOffset(), payload.limit());

                Object result = resultReader.readFrom(bin);

                logger.trace(marker, "Deserialized result: {}", result);
                received.add(1, attributes);
                resultQueue.remove().complete(result);
            }
        } catch (Throwable e) {
            logger.error(marker, "Could not read result", e);
            resultQueue.remove().completeExceptionally(e);
        }

        checkDone();
    }

    @Override
    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        WithResult<?, Object> withResult = (WithResult<?, Object>) item;
        CompletableFuture<Object> result = withResult.result().toCompletableFuture();
        resultQueue.add(result);

        eventWriter.writeTo(withResult.event(), outputStream);
    }
}
