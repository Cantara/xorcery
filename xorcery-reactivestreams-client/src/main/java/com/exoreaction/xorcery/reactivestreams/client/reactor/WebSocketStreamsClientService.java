package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketStreamsClient;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.HttpCookie;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

@Service(name = "reactivestreams.client.reactor")
@ContractsProvided({WebSocketStreamsClient.class})
public class WebSocketStreamsClientService
        implements WebSocketStreamsClient {

    private final WebSocketClient webSocketClient;
    private final MessageWorkers messageWorkers;
    private final DnsLookupService dnsLookup;
    private final Logger logger;
    private final Tracer tracer;
    private final TextMapPropagator textMapPropagator;
    private final ByteBufferPool byteBufferPool;
    private final Meter meter;

    @Inject
    public WebSocketStreamsClientService(
            Configuration configuration,
            MessageWorkers messageWorkers,
            HttpClient httpClient,
            DnsLookupService dnsLookup,
            OpenTelemetry openTelemetry,
            Logger logger
    ) throws Exception {
        this.messageWorkers = messageWorkers;
        this.dnsLookup = dnsLookup;
        this.logger = logger;
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
    public <PUBLISH> Flux<PUBLISH> publish(URI serverUri, String contentType, Class<PUBLISH> publishType, WebSocketClientOptions options, Publisher<PUBLISH> publisher) {
        validateUri(serverUri);
        MessageWriter<PUBLISH> messageWriter = messageWorkers.newWriter(publishType, publishType, contentType);
        return Flux.<PUBLISH>push(sink -> new ClientWebSocketStream<PUBLISH, PUBLISH>(
                serverUri, contentType,
                messageWriter, null,
                publisher,
                sink,
                options,
                dnsLookup, webSocketClient, byteBufferPool, meter, tracer, textMapPropagator, logger));
    }

    @Override
    public <PUBLISH, RESULT> Flux<RESULT> publishWithResult(URI serverUri, String contentType, Class<PUBLISH> publishType, Class<RESULT> resultType, WebSocketClientOptions options, Publisher<PUBLISH> publisher) {
        validateUri(serverUri);
        MessageWriter<PUBLISH> messageWriter = messageWorkers.newWriter(publishType, publishType, contentType);
        MessageReader<RESULT> messageReader = messageWorkers.newReader(resultType, resultType, contentType);
        return Flux.push(sink -> new ClientWebSocketStream<>(
                serverUri, contentType,
                messageWriter, messageReader,
                publisher,
                sink,
                options,
                dnsLookup, webSocketClient, byteBufferPool, meter, tracer, textMapPropagator, logger));
    }

    @Override
    public <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(URI serverUri, String contentType, Class<SUBSCRIBE> subscribeType, WebSocketClientOptions options) {
        validateUri(serverUri);
        MessageReader<SUBSCRIBE> messageReader = messageWorkers.newReader(subscribeType, subscribeType, contentType);

        return Flux.create(sink ->
                new ClientWebSocketStream<SUBSCRIBE, SUBSCRIBE>(
                        serverUri,
                        contentType,
                        null,
                        messageReader,
                        null,
                        sink,
                        options,
                        dnsLookup,
                        webSocketClient,
                        byteBufferPool,
                        meter,
                        tracer,
                        textMapPropagator,
                        logger));
    }

    private void validateUri(URI serverUri)
            throws IllegalArgumentException {
        if (!("ws".equals(serverUri.getScheme()) || "wss".equals(serverUri.getScheme()) || "srv".equals(serverUri.getScheme()))) {
            throw new IllegalArgumentException("URI scheme " + serverUri.getScheme() + " not supported. Must be one of 'ws', 'wss', or 'srv'");
        }
    }
}
