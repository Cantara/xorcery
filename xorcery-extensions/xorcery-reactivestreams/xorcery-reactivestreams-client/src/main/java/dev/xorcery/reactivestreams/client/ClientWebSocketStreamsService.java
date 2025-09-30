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
package dev.xorcery.reactivestreams.client;

import dev.xorcery.collections.Element;
import dev.xorcery.concurrent.NamedThreadFactory;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.dns.client.api.DnsLookup;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import dev.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import dev.xorcery.reactivestreams.spi.MessageWorkers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SchemaUrls;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientWebSocketStreamsService
        implements ClientWebSocketStreams {

    private final WebSocketClient webSocketClient;
    private final MessageWorkers messageWorkers;
    private final DnsLookup dnsLookup;
    private final LoggerContext loggerContext;
    private final Logger logger;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final ByteBufferPool byteBufferPool;
    private final Meter meter;
    private final String host;
    private final ExecutorService flushingExecutors = Executors.newCachedThreadPool(new NamedThreadFactory("reactivestreams-client-flusher-"));

    public ClientWebSocketStreamsService(
            Configuration configuration,
            MessageWorkers messageWorkers,
            HttpClient httpClient,
            DnsLookup dnsLookup,
            OpenTelemetry openTelemetry,
            LoggerContext loggerContext
    ) throws Exception {
        this.messageWorkers = messageWorkers;
        this.dnsLookup = dnsLookup;
        this.loggerContext = loggerContext;
        this.logger = loggerContext.getLogger(ClientWebSocketStreamsService.class);
        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        ClientWebSocketStreamConfiguration.get(configuration).configure(webSocketClient);
        webSocketClient.start();
        this.webSocketClient = webSocketClient;
        this.host = InstanceConfiguration.get(configuration).getHost();
        byteBufferPool = new ArrayByteBufferPool();

        tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        meter = openTelemetry.meterBuilder(ClientWebSocketStream.class.getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public <PUBLISH> CompletableFuture<Void> publish(Flux<PUBLISH> publisher, URI serverUri, ClientWebSocketOptions options, Class<? super PUBLISH> publishType, String... publishContentTypes) {
        Collection<String> availableContentTypes = messageWorkers.getAvailableWriteContentTypes(publishType, Arrays.asList(publishContentTypes));
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageWriter implementation for given published type and content types");
        }

        CompletableFuture<Void> result = new CompletableFuture<>();
        new ClientSubscriberSubProtocolHandler<PUBLISH>(
                publisher,
                serverUri,
                result,
                options,
                publishType,
                availableContentTypes,
                messageWorkers,
                dnsLookup,
                webSocketClient,
                flushingExecutors,
                host,
                byteBufferPool,
                meter,
                tracer,
                textMapPropagator,
                loggerContext.getLogger(ClientSubscriberSubProtocolHandler.class));
        return result;
    }

    @Override
    public <PUBLISH, RESULT> Flux<RESULT> publishWithResult(Flux<PUBLISH> publisher, URI serverUri, ClientWebSocketOptions options, Class<? super PUBLISH> publishType, Class<? super RESULT> resultType, Collection<String> messageContentTypes, Collection<String> resultContentTypes) {
        Collection<String> writeTypes = messageWorkers.getAvailableWriteContentTypes(publishType, messageContentTypes);
        if (writeTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageWriter implementation for given published type and content types");
        }
        Collection<String> readTypes = messageWorkers.getAvailableReadContentTypes(resultType, resultContentTypes);
        if (readTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageReader implementation for given result type and content types");
        }
        return Flux.create(inboundSink -> new ClientSubscriberWithResultSubProtocolHandler<PUBLISH, RESULT>(
                publisher,
                serverUri,
                inboundSink,
                options,
                publishType,
                resultType,
                writeTypes,
                readTypes,
                messageWorkers,
                dnsLookup,
                webSocketClient,
                flushingExecutors,
                host,
                byteBufferPool,
                meter,
                tracer,
                textMapPropagator,
                loggerContext.getLogger(ClientSubscriberWithResultSubProtocolHandler.class)));
    }

    @Override
    public <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(URI serverUri, ClientWebSocketOptions options, Class<? super SUBSCRIBE> subscribeType, String... messageContentTypes) {
        Collection<String> readTypes = messageWorkers.getAvailableReadContentTypes(subscribeType, Arrays.asList(messageContentTypes));
        if (readTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageReader implementation for given result type and content types");
        }

        return Flux.create(inboundSink -> new ClientPublisherSubProtocolHandler<>(
                        serverUri,
                        inboundSink,
                        options,
                        subscribeType,
                        readTypes,
                        messageWorkers,
                        dnsLookup,
                        webSocketClient,
                        flushingExecutors,
                        host,
                        byteBufferPool,
                        meter,
                        tracer,
                        textMapPropagator,
                        loggerContext.getLogger(ClientSubscriberWithResultSubProtocolHandler.class)));
    }

    public void preDestroy() {
        try {
            webSocketClient.stop();
            webSocketClient.getHttpClient().stop();
            flushingExecutors.shutdown();
        } catch (Exception e) {
            logger.warn("Could not stop websocket client", e);
        }
    }

    private URI getServerUri(ContextView contextView) {
        Object serverUri = new ContextViewElement(contextView).get(ClientWebSocketStreamContext.serverUri)
                .orElseThrow(Element.missing(ClientWebSocketStreamContext.serverUri));
        return validateUri(serverUri instanceof URI uri ? uri : URI.create(serverUri.toString()));
    }

    private URI validateUri(URI serverUri)
            throws IllegalArgumentException {
        if (!("ws".equals(serverUri.getScheme()) || "wss".equals(serverUri.getScheme()) || "srv".equals(serverUri.getScheme()))) {
            throw new IllegalArgumentException("URI scheme " + serverUri.getScheme() + " not supported. Must be one of 'ws', 'wss', or 'srv'");
        }
        return serverUri;
    }
}
