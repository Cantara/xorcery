package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

public class WebSocketStreamsClientService
        implements WebSocketStreamsClient {

    private final WebSocketClient webSocketClient;
    private final MessageWorkers messageWorkers;
    private final DnsLookup dnsLookup;
    private final LoggerContext loggerContext;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final ByteBufferPool byteBufferPool;
    private final Meter meter;

    public WebSocketStreamsClientService(
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
        WebSocketClient webSocketClient = new WebSocketClient(httpClient);
        WebSocketStreamClientConfiguration.get(configuration).configure(webSocketClient);
        webSocketClient.start();
        this.webSocketClient = webSocketClient;
        byteBufferPool = new ArrayByteBufferPool();

        tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Override
    public <PUBLISH> Flux<PUBLISH> publish(URI serverUri, WebSocketClientOptions options, Class<? super PUBLISH> publishType, Publisher<PUBLISH> publisher, String... publishContentTypes) {
        validateUri(serverUri);

        Collection<String> availableContentTypes = messageWorkers.getAvailableWriteContentTypes(publishType, Arrays.asList(publishContentTypes));
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageWriter implementation for given published type and content types");
        }

        return Flux.create(sink -> new ClientWebSocketStream<>(
                serverUri,
                ReactiveStreamSubProtocol.subscriber,
                availableContentTypes, null,
                publishType,
                null,
                messageWorkers,
                publisher,
                sink,
                options,
                dnsLookup, webSocketClient, byteBufferPool, meter, tracer, textMapPropagator, loggerContext.getLogger(ClientWebSocketStream.class)));
    }

    @Override
    public <PUBLISH, RESULT> Flux<RESULT> publishWithResult(URI serverUri, WebSocketClientOptions options, Class<? super PUBLISH> publishType, Class<? super RESULT> resultType, Publisher<PUBLISH> publisher, Collection<String> publishContentTypes, Collection<String> resultContentTypes) {
        validateUri(serverUri);

        Collection<String> availableContentTypes = messageWorkers.getAvailableWriteContentTypes(publishType, publishContentTypes);
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageWriter implementation for given published type and content types");
        }
        Collection<String> availableResultContentTypes = messageWorkers.getAvailableReadContentTypes(resultType, resultContentTypes);
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageReader implementation for given result type and content types");
        }
        return Flux.create(sink -> new ClientWebSocketStream<>(
                serverUri,
                ReactiveStreamSubProtocol.subscriberWithResult,
                availableContentTypes, availableResultContentTypes,
                publishType,resultType,
                messageWorkers,
                publisher,
                sink,
                options,
                dnsLookup, webSocketClient, byteBufferPool, meter, tracer, textMapPropagator, loggerContext.getLogger(ClientWebSocketStream.class)));
    }

    @Override
    public <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(URI serverUri, WebSocketClientOptions options, Class<? super SUBSCRIBE> subscribeType, String... messageContentTypes) {
        validateUri(serverUri);

        Collection<String> availableResultContentTypes = messageWorkers.getAvailableReadContentTypes(subscribeType, Arrays.asList(messageContentTypes));
        if (availableResultContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageReader implementation for given result type and content types");
        }

        return Flux.create(sink ->
                new ClientWebSocketStream<SUBSCRIBE, SUBSCRIBE>(
                        serverUri,
                        ReactiveStreamSubProtocol.publisher,
                        null, availableResultContentTypes,
                        null, subscribeType,
                        messageWorkers,
                        null,
                        sink,
                        options,
                        dnsLookup,
                        webSocketClient,
                        byteBufferPool,
                        meter,
                        tracer,
                        textMapPropagator,
                        loggerContext.getLogger(ClientWebSocketStream.class)));
    }

    private void validateUri(URI serverUri)
            throws IllegalArgumentException {
        if (!("ws".equals(serverUri.getScheme()) || "wss".equals(serverUri.getScheme()) || "srv".equals(serverUri.getScheme()))) {
            throw new IllegalArgumentException("URI scheme " + serverUri.getScheme() + " not supported. Must be one of 'ws', 'wss', or 'srv'");
        }
    }
}
