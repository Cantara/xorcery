package com.exoreaction.xorcery.reactivestreams.server.reactor;


import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketServerOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.WebSocketStreamsServer;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.JettyServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

@Service(name = "reactivestreams.server.reactor")
@ContractsProvided({WebSocketStreamsServer.class})
@RunLevel(4)
public class WebSocketStreamsServerService
        implements WebSocketStreamsServer {

    protected static final TextMapGetter<UpgradeRequest> jettyGetter =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(UpgradeRequest context) {
                    return context.getHeaders().keySet();
                }

                @Override
                public String get(UpgradeRequest context, String key) {
                    return context.getHeader(key);
                }
            };


    private final MessageWorkers messageWorkers;
    private final LoggerContext loggerContext;
    private final Logger logger;
    private JettyWebSocketServerContainer container;

    protected final ByteBufferPool byteBufferPool;


    // Subprotocol -> Path -> Content type -> handler
    private final Map<String, Map<String, Map<String, WebSocketHandler>>> handlers = new ConcurrentHashMap<>();

    private final Set<String> mappedPaths = new CopyOnWriteArraySet<>();

    private final TextMapPropagator textMapPropagator;
    private final Meter meter;
    private final Tracer tracer;

    private final StreamWebSocketCreator webSocketCreator = new StreamWebSocketCreator();

    @Inject
    public WebSocketStreamsServerService(
            Configuration configuration,
            MessageWorkers messageWorkers,
            ServletContextHandler servletContextHandler,
            OpenTelemetry openTelemetry,
            LoggerContext loggerContext) {
        this.messageWorkers = messageWorkers;
        this.loggerContext = loggerContext;

        WebSocketStreamsServerConfiguration serverConfiguration = WebSocketStreamsServerConfiguration.get(configuration);

        JettyWebSocketServletContainerInitializer.configure(servletContextHandler, (servletContext, container) ->
        {
            serverConfiguration.apply(container);
            WebSocketStreamsServerService.this.container = container;
        });

        this.logger = loggerContext.getLogger(getClass());

        this.byteBufferPool = new ArrayByteBufferPool();
        textMapPropagator = openTelemetry.getPropagators().getTextMapPropagator();
        meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
    }

    public JettyWebSocketServerContainer getContainer() {
        return container;
    }

    @Override
    public <PUBLISH> Disposable publisher(String path, String contentType, Class<PUBLISH> publishType, Publisher<PUBLISH> publisher) throws IllegalArgumentException {
        MessageWriter<Object> itemWriter = Optional.ofNullable(messageWorkers.newWriter(publishType, publishType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + publishType.getTypeName() + "(" + contentType + ")"));

        return registerHandler("publisher", path, contentType, null, itemWriter, flux -> Flux.from(publisher));
    }

    @Override
    public <SUBSCRIBE> Disposable subscriber(String path, String contentType, Class<SUBSCRIBE> subscribeType, Function<Flux<SUBSCRIBE>, Publisher<SUBSCRIBE>> appendSubscriber) throws IllegalArgumentException {
        MessageReader<SUBSCRIBE> itemReader = Optional.ofNullable(messageWorkers.<SUBSCRIBE>newReader(subscribeType, subscribeType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + subscribeType.getTypeName() + "(" + contentType + ")"));

        return registerHandler("subscriber", path, contentType, itemReader, null, appendSubscriber);
    }

    @Override
    public <SUBSCRIBE, RESULT> Disposable subscriberWithResult(String path, String contentType, Class<SUBSCRIBE> subscribeType, Class<RESULT> resultType, Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeAndReturnResult) throws IllegalArgumentException {

        MessageReader<SUBSCRIBE> itemReader = Optional.ofNullable(messageWorkers.<SUBSCRIBE>newReader(subscribeType, subscribeType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + subscribeType.getTypeName() + "(" + contentType + ")"));
        MessageWriter<RESULT> itemWriter = Optional.ofNullable(messageWorkers.<RESULT>newWriter(resultType, resultType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + resultType.getTypeName() + "(" + contentType + ")"));

        return registerHandler("subscriberWithResult", path, contentType, itemReader, itemWriter, subscribeAndReturnResult);
    }

    private <INPUT, OUTPUT> Disposable registerHandler(String subProtocol, String path, String contentType, MessageReader<INPUT> reader, MessageWriter<OUTPUT> writer, Function<Flux<INPUT>, Publisher<OUTPUT>> handler) {

        if (!path.contains("|") && !path.startsWith("/")) {
            path = "/" + path;
        }

        Map<String, WebSocketHandler> contentTypeHandlers = handlers
                .computeIfAbsent(subProtocol, p -> new ConcurrentHashMap<>())
                .computeIfAbsent(path, p ->
                {
                    if (mappedPaths.add(p)) {
                        // Always register as URI template
                        container.addMapping("uri-template|"+p, webSocketCreator);
                    }
                    return new ConcurrentHashMap<>();
                });

        if (contentTypeHandlers.containsKey(contentType))
            throw new IllegalArgumentException("Subscriber for " + path + " and content type " + contentType + " already exists");
        contentTypeHandlers.put(contentType, new WebSocketHandler(reader, writer, (Function) handler));

        return () -> contentTypeHandlers.remove(contentType);
    }

    record WebSocketHandler(MessageReader<?> reader, MessageWriter<?> writer,
                            Function<Flux<Object>, Publisher<Object>> handler) {
    }

    class StreamWebSocketCreator
            implements JettyWebSocketCreator {

        @Override
        public Object createWebSocket(JettyServerUpgradeRequest jettyServerUpgradeRequest, JettyServerUpgradeResponse jettyServerUpgradeResponse) {
            try {
                for (String requestSubProtocol : jettyServerUpgradeRequest.getSubProtocols()) {
                    Map<String, Map<String, WebSocketHandler>> pathHandlers = handlers.get(requestSubProtocol);
                    if (pathHandlers == null)
                        continue;

                    String path = jettyServerUpgradeRequest.getRequestPath();
                    Map<String, WebSocketHandler> contentTypeHandlers;
                    if (jettyServerUpgradeRequest.getServletAttribute("org.eclipse.jetty.http.pathmap.PathSpec") instanceof UriTemplatePathSpec pathSpec) {
                        String factoryPath = pathSpec.getDeclaration();
                        contentTypeHandlers = pathHandlers.get(factoryPath);
                    } else {
                        contentTypeHandlers = pathHandlers.get(path);
                    }

                    if (contentTypeHandlers == null) {
                        jettyServerUpgradeResponse.sendError(HttpStatus.NOT_FOUND_404, "Not found");
                        return null;
                    }

                    String contentType = jettyServerUpgradeRequest.getHeader(HttpHeader.CONTENT_TYPE.asString());
                    WebSocketHandler webSocketHandler;
                    if (contentType == null) {
                        if (contentTypeHandlers.size() == 1) {
                            webSocketHandler = contentTypeHandlers.values().iterator().next();
                        } else {
                            jettyServerUpgradeResponse.sendError(HttpStatus.NOT_ACCEPTABLE_406, "Media type not specified");
                            return null;
                        }
                    } else {
                        webSocketHandler = contentTypeHandlers.get(contentType);
                        if (webSocketHandler == null) {
                            jettyServerUpgradeResponse.sendError(HttpStatus.NOT_ACCEPTABLE_406, "Media type " + contentType + " not supported");
                            return null;
                        }
                    }

                    Context context = textMapPropagator.extract(Context.current(), jettyServerUpgradeRequest, jettyGetter);

                    Function<Flux<Object>, Publisher<Object>> handler = webSocketHandler.handler();

                    jettyServerUpgradeResponse.setAcceptedSubProtocol(requestSubProtocol);

                    return new ServerWebSocketStream<>(
                            path,
                            WebSocketServerOptions.instance(),
                            (MessageWriter<Object>) webSocketHandler.writer(),
                            (MessageReader<Object>) webSocketHandler.reader(),
                            handler,
                            byteBufferPool,
                            loggerContext.getLogger(ServerWebSocketStream.class),
                            tracer,
                            meter,
                            context
                    );
                }

                // No protocols or handlers found matching this request
                jettyServerUpgradeResponse.sendError(HttpStatus.NOT_FOUND_404, "Not found");
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
