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
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.common.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class PublishWithResultReactiveStreamStandard
        extends PublishReactiveStreamStandard {
    private final static Logger logger = LogManager.getLogger(PublishWithResultReactiveStreamStandard.class);
    private final MessageReader<Object> resultReader;

    private final Queue<CompletableFuture<Object>> resultQueue = new ConcurrentLinkedQueue<>();
    private final Meter received;

    public PublishWithResultReactiveStreamStandard(URI serverUri,
                                                   String streamName,
                                                   ClientConfiguration publisherConfiguration,
                                                   DnsLookup dnsLookup,
                                                   WebSocketClient webSocketClient,
                                                   Flow.Publisher<Object> publisher,
                                                   MessageWriter<Object> eventWriter,
                                                   MessageReader<Object> resultReader,
                                                   Supplier<Configuration> subscriberConfiguration,
                                                   ByteBufferPool pool,
                                                   MetricRegistry metricRegistry,
                                                   CompletableFuture<Void> result) {
        super(serverUri, streamName, publisherConfiguration, dnsLookup, webSocketClient, publisher, eventWriter, subscriberConfiguration, pool, metricRegistry, result);
        this.resultReader = resultReader;
        this.received = metricRegistry.meter("publish." + streamName + ".received");

    }


    @Override
    public void retry(Throwable cause) {
        resultQueue.clear();
        super.retry(cause);
    }

    @Override
    protected void checkDone() {
        if (isComplete && resultQueue.isEmpty()) {
            result.complete(null);
            logger.info(marker, "Sending complete for session {}", session.getRemote().getRemoteAddress());
            session.close(StatusCode.NORMAL, "complete");
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onWebSocketBinary");
        }

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

        checkDone();
    }

    @Override
    protected void writeEvent(Object item) throws IOException {
        WithResult<?, Object> withResult = (WithResult<?, Object>) item;
        CompletableFuture<Object> result = withResult.result().toCompletableFuture();
        resultQueue.add(result);

        eventWriter.writeTo(withResult.event(), outputStream);
    }
}
