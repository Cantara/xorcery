package com.exoreaction.xorcery.reactivestreams.server.reactor;


import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
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
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.pathmap.PathSpec;
import org.eclipse.jetty.http.pathmap.UriTemplatePathSpec;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.api.ExtensionConfig;
import org.eclipse.jetty.websocket.server.*;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

@Service(name = "reactivestreams.server.reactor")
@ContractsProvided({ServerWebSocketStreams.class})
@RunLevel(4)
public class ServerWebSocketStreamsService
        implements ServerWebSocketStreams {

    protected static final TextMapGetter<ServerUpgradeRequest> jettyGetter =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(ServerUpgradeRequest context) {
                    return context.getHeaders().getFieldNamesCollection();
                }

                @Override
                public String get(ServerUpgradeRequest context, String key) {
                    return context.getHeaders().get(key);
                }
            };


    private final MessageWorkers messageWorkers;
    private final LoggerContext loggerContext;
    private final Logger logger;
    private final WebSocketUpgradeHandler webSocketUpgradeHandler;

    protected final ByteBufferPool byteBufferPool;
    private final ExecutorService flushingExecutors = Executors.newCachedThreadPool(new NamedThreadFactory("reactivestreams-server-flusher-"));

    // Path -> Subprotocol -> handler
    private final Map<String, Map<ReactiveStreamSubProtocol, WebSocketHandler>> pathHandlers = new ConcurrentHashMap<>();
    private final Set<String> mappedPaths = new CopyOnWriteArraySet<>();

    private final TextMapPropagator textMapPropagator;
    private final Meter meter;
    private final Tracer tracer;

    private final StreamWebSocketCreator webSocketCreator = new StreamWebSocketCreator();

    @Inject
    public ServerWebSocketStreamsService(
            MessageWorkers messageWorkers,
            WebSocketUpgradeHandler webSocketUpgradeHandler,
            OpenTelemetry openTelemetry,
            LoggerContext loggerContext) {
        this.messageWorkers = messageWorkers;
        this.webSocketUpgradeHandler = webSocketUpgradeHandler;
        this.loggerContext = loggerContext;

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

    @Override
    public <PUBLISH> Disposable publisher(String path, Class<? super PUBLISH> publishType, Publisher<PUBLISH> publisher) throws IllegalArgumentException {

        return registerHandler(ReactiveStreamSubProtocol.publisher, path, null, publishType, flux -> Flux.from(publisher));
    }

    @Override
    public <SUBSCRIBE> Disposable subscriber(String path, Class<? super SUBSCRIBE> subscribeType, Function<Flux<SUBSCRIBE>, Publisher<SUBSCRIBE>> appendSubscriber) throws IllegalArgumentException {
        return registerHandler(ReactiveStreamSubProtocol.subscriber, path, subscribeType, null, appendSubscriber);
    }

    @Override
    public <SUBSCRIBE, RESULT> Disposable subscriberWithResult(String path, Class<? super SUBSCRIBE> subscribeType, Class<? super RESULT> resultType, Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeAndReturnResult) throws IllegalArgumentException {
        return registerHandler(ReactiveStreamSubProtocol.subscriberWithResult, path, subscribeType, resultType, subscribeAndReturnResult);
    }

    private <INPUT, OUTPUT> Disposable registerHandler(ReactiveStreamSubProtocol subProtocol, String path, Class<?> readerType, Class<?> writerType, Function<Flux<INPUT>, Publisher<OUTPUT>> handler) {

        if (!path.contains("|") && !path.startsWith("/")) {
            path = "/" + path;
        }

        Map<ReactiveStreamSubProtocol, WebSocketHandler> subProtocolHandlers = pathHandlers.computeIfAbsent(path, p -> new ConcurrentHashMap<>());

        if (subProtocolHandlers.containsKey(subProtocol))
            throw new IllegalArgumentException("Handler for " + path + " and subprotocol " + subProtocol + " already exists");

        // Only register once for each path
        if (mappedPaths.add(path)) {
            // Always register as URI template
            webSocketUpgradeHandler.getServerWebSocketContainer().addMapping("uri-template|" + path, webSocketCreator);
        }

        subProtocolHandlers.put(subProtocol, new WebSocketHandler(subProtocol, readerType, writerType, (Function) handler));

        return () -> subProtocolHandlers.remove(subProtocol);
    }

    record WebSocketHandler(ReactiveStreamSubProtocol subProtocol,
                            Class<?> readerType,
                            Class<?> writerType,
                            Function<Flux<Object>, Publisher<Object>> handler) {
    }

    class StreamWebSocketCreator
            implements WebSocketCreator {

        @Override
        public Object createWebSocket(ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse, Callback callback) {
            for (String requestSubProtocolString : serverUpgradeRequest.getSubProtocols()) {

                ReactiveStreamSubProtocol requestSubProtocol = null;
                try {
                    requestSubProtocol = ReactiveStreamSubProtocol.valueOf(requestSubProtocolString);
                } catch (IllegalArgumentException e) {
                    serverUpgradeResponse.setStatus(HttpStatus.BAD_REQUEST_400);
                    return null;
                }

                String path = serverUpgradeRequest.getHttpURI().getPath();

                Map<String, String> pathParameters = Collections.emptyMap();
                Map<ReactiveStreamSubProtocol, WebSocketHandler> subProtocolHandlers;
                if (serverUpgradeRequest.getAttribute(PathSpec.class.getName()) instanceof UriTemplatePathSpec pathSpec) {
                    pathParameters = pathSpec.getPathParams(path);
                    String factoryPath = pathSpec.getDeclaration();
                    subProtocolHandlers = pathHandlers.get(factoryPath);
                } else {
                    subProtocolHandlers = pathHandlers.get(path);
                }
                if (subProtocolHandlers == null)
                    continue;
                WebSocketHandler subProtocolHandler = subProtocolHandlers.get(requestSubProtocol);
                if (subProtocolHandler == null)
                    continue;

                // Content type negotiation for both read (Content-Type header) and write (Accept header)
                MessageReader<Object> reader = null;
                MessageWriter<Object> writer = null;
                switch (requestSubProtocol) {
                    case publisher -> {
                        writer = getWriter(subProtocolHandler.writerType(), serverUpgradeRequest, serverUpgradeResponse);
                        if (writer == null)
                            return null;
                    }
                    case subscriber -> {
                        reader = getReader(subProtocolHandler.readerType(), serverUpgradeRequest, serverUpgradeResponse);
                        if (reader == null)
                            return null;
                    }
                    case subscriberWithResult -> {
                        reader = getReader(subProtocolHandler.readerType(), serverUpgradeRequest, serverUpgradeResponse);
                        if (reader == null)
                            return null;
                        writer = getWriter(subProtocolHandler.writerType(), serverUpgradeRequest, serverUpgradeResponse);
                        if (writer == null)
                            return null;

                    }
                }

                Context context = textMapPropagator.extract(Context.current(), serverUpgradeRequest, jettyGetter);
                Function<Flux<Object>, Publisher<Object>> handler = subProtocolHandler.handler();
                serverUpgradeResponse.setAcceptedSubProtocol(requestSubProtocol.name());
                serverUpgradeResponse.getHeaders().add("Aggregate", "maxBinaryMessageSize="+webSocketUpgradeHandler.getServerWebSocketContainer().getMaxBinaryMessageSize());

                // Aggregate header for batching support
                int clientMaxBinaryMessageSize = Optional.ofNullable(serverUpgradeRequest.getHeaders().get("Aggregate")).map(header ->
                {
                    Map<String, String> parameters = new HashMap<>();
                    Arrays.asList(header.split(";")).forEach(parameter -> {
                        String[] paramKeyValue = parameter.split("=");
                        parameters.put(paramKeyValue[0], paramKeyValue[1]);
                    });
                    return Integer.valueOf(parameters.computeIfAbsent("maxBinaryMessageSize", k -> "-1"));
                }).orElse(-1);


                return new ServerWebSocketStream<>(
                        path,
                        pathParameters,
                        ServerWebSocketOptions.instance(),
                        writer,
                        reader,
                        handler,
                        flushingExecutors,
                        byteBufferPool,
                        clientMaxBinaryMessageSize,
                        loggerContext.getLogger(ServerWebSocketStream.class),
                        tracer,
                        meter,
                        context
                );
            }

            // No protocols or handlers found matching this request
            serverUpgradeResponse.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }

        private MessageWriter<Object> getWriter(Class<?> type, ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse) {
            List<String> availableWriteContentTypes = Collections.emptyList();
            HttpField acceptField = serverUpgradeRequest.getHeaders().getField(HttpHeader.ACCEPT);
            if (acceptField != null) {
                availableWriteContentTypes = acceptField.getValueList();
            }

            for (String contentType : availableWriteContentTypes) {

                MessageWriter<Object> writer = messageWorkers.newWriter(type, type, contentType);
                if (writer != null) {
                    serverUpgradeResponse.getHeaders().add(HttpHeader.CONTENT_TYPE, contentType);
                    return writer;
                }
            }

            // No content type handlers found
            serverUpgradeResponse.setStatus(HttpStatus.NOT_ACCEPTABLE_406);
            return null;
        }

        private MessageReader<Object> getReader(Class<?> type, ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse) {
            List<String> availableReadContentTypes = Collections.emptyList();
            HttpField contentTypeField = serverUpgradeRequest.getHeaders().getField(HttpHeader.CONTENT_TYPE);
            if (contentTypeField != null) {
                availableReadContentTypes = contentTypeField.getValueList();
            }

            for (String contentType : availableReadContentTypes) {

                MessageReader<Object> reader = messageWorkers.newReader(type, type, contentType);
                if (reader != null) {
                    serverUpgradeResponse.getHeaders().add(HttpHeader.ACCEPT, contentType);
                    return reader;
                }
            }

            // No content type handlers found
            serverUpgradeResponse.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415);
            return null;
        }
    }
}
