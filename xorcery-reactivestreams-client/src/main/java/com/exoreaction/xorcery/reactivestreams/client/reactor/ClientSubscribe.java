package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.reactivestreams.api.client.WebSocketClientOptions;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.Disposable;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.net.HttpCookie;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

class ClientSubscribe<INPUT, OUTPUT>
        extends ConnectableFlux<OUTPUT> {

    private final URI serverUri;
    private final Function<INPUT, Publisher<OUTPUT>> flatMapper;
    private final String mediaType;
    private final WebSocketClientOptions options;
    private final MessageReader<INPUT> reader;
    private final MessageWriter<OUTPUT> writer;

    private final DnsLookup dnsLookup;
    private final WebSocketClient webSocketClient;

    private final ByteBufferPool byteBufferPool;
    private final Tracer tracer;
    private final Meter meter;
    private final TextMapPropagator textMapPropagator;
    private final Logger logger;

    private Consumer<? super Disposable> cancelSupport;
    private Flux<OUTPUT> mappedClient;

    public ClientSubscribe(
            URI serverUri,
            MessageReader<INPUT> reader,
            MessageWriter<OUTPUT> writer,
            Function<INPUT, Publisher<OUTPUT>> flatMapper,
            String mediaType,

            WebSocketClientOptions options,

            DnsLookup dnsLookup,
            WebSocketClient webSocketClient,
            ByteBufferPool byteBufferPool,

            Tracer tracer,
            Meter meter,
            TextMapPropagator textMapPropagator,
            Logger logger) {
        this.serverUri = serverUri;
        this.reader = reader;
        this.writer = writer;
        this.flatMapper = flatMapper;
        this.mediaType = mediaType;
        this.options = options;
        this.dnsLookup = dnsLookup;
        this.webSocketClient = webSocketClient;
        this.byteBufferPool = byteBufferPool;
        this.tracer = tracer;
        this.meter = meter;
        this.textMapPropagator = textMapPropagator;
        this.logger = logger;
    }

    @Override
    public void connect(Consumer<? super Disposable> cancelSupport) {
        this.cancelSupport = cancelSupport;

        Flux<INPUT> client = Flux.<INPUT>create(sink ->
        new ClientWebSocketStream<OUTPUT, INPUT>(
                serverUri,
                mediaType,
                writer,
                reader,
                this,
                sink,
                options,
                dnsLookup,
                webSocketClient,
                byteBufferPool,
                meter,
                tracer,
                textMapPropagator,
                logger));

        mappedClient = client.flatMap(flatMapper);
    }

    @Override
    public void subscribe(CoreSubscriber<? super OUTPUT> actual) {
        cancelSupport.accept(mappedClient.subscribe());
    }
}
