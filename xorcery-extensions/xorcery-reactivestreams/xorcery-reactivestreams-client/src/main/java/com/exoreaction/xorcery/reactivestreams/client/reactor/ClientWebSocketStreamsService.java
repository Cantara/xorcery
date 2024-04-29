package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public class ClientWebSocketStreamsService
        implements ClientWebSocketStreams {

    private final WebSocketClient webSocketClient;
    private final MessageWorkers messageWorkers;
    private final DnsLookup dnsLookup;
    private final LoggerContext loggerContext;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final ByteBufferPool byteBufferPool;
    private final Meter meter;

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
    public <PUBLISH> Function<Flux<PUBLISH>, Publisher<PUBLISH>> publish(ClientWebSocketOptions options, Class<? super PUBLISH> publishType, String... publishContentTypes) {
        Collection<String> availableContentTypes = messageWorkers.getAvailableWriteContentTypes(publishType, Arrays.asList(publishContentTypes));
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageWriter implementation for given published type and content types");
        }

        return flux -> Flux.create(sink -> new ClientWebSocketStream<>(
                getServerUri(sink.contextView()),
                ReactiveStreamSubProtocol.subscriber,
                availableContentTypes, null,
                publishType,
                null,
                messageWorkers,
                flux,
                sink,
                options,
                dnsLookup, webSocketClient, byteBufferPool, meter, tracer, textMapPropagator, loggerContext.getLogger(ClientWebSocketStream.class)));
    }

    @Override
    public <PUBLISH, RESULT> Function<Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(ClientWebSocketOptions options, Class<? super PUBLISH> publishType, Class<? super RESULT> resultType, Collection<String> messageContentTypes, Collection<String> resultContentTypes) {
        Collection<String> availableContentTypes = messageWorkers.getAvailableWriteContentTypes(publishType, messageContentTypes);
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageWriter implementation for given published type and content types");
        }
        Collection<String> availableResultContentTypes = messageWorkers.getAvailableReadContentTypes(resultType, resultContentTypes);
        if (availableContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageReader implementation for given result type and content types");
        }
        return flux -> Flux.create(sink -> new ClientWebSocketStream<>(
                getServerUri(sink.contextView()),
                ReactiveStreamSubProtocol.subscriberWithResult,
                availableContentTypes, availableResultContentTypes,
                publishType, resultType,
                messageWorkers,
                flux,
                sink,
                options,
                dnsLookup, webSocketClient, byteBufferPool, meter, tracer, textMapPropagator, loggerContext.getLogger(ClientWebSocketStream.class)));
    }

    @Override
    public <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(ClientWebSocketOptions options, Class<? super SUBSCRIBE> subscribeType, String... messageContentTypes) {
        Collection<String> availableResultContentTypes = messageWorkers.getAvailableReadContentTypes(subscribeType, Arrays.asList(messageContentTypes));
        if (availableResultContentTypes.isEmpty()) {
            throw new IllegalArgumentException("No MessageReader implementation for given result type and content types");
        }

        return Flux.create(sink ->
                new ClientWebSocketStream<SUBSCRIBE, SUBSCRIBE>(
                        getServerUri(sink.contextView()),
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

    private URI getServerUri(ContextView contextView) {
        Object serverUri = new ContextViewElement(contextView).get(ClientWebSocketStreamContext.serverUri)
                .orElseThrow(ContextViewElement.missing(ClientWebSocketStreamContext.serverUri));
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
