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
package dev.xorcery.reactivestreams.server;


import dev.xorcery.concurrent.NamedThreadFactory;
import dev.xorcery.reactivestreams.api.ReactiveStreamSubProtocol;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketOptions;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import dev.xorcery.reactivestreams.spi.MessageReader;
import dev.xorcery.reactivestreams.spi.MessageWorkers;
import dev.xorcery.reactivestreams.spi.MessageWriter;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.semconv.SchemaUrls;
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
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.server.ServerUpgradeRequest;
import org.eclipse.jetty.websocket.server.ServerUpgradeResponse;
import org.eclipse.jetty.websocket.server.WebSocketCreator;
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static dev.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.OPEN_CONNECTIONS;

@Service(name = "reactivestreams.server")
@ContractsProvided({ServerWebSocketStreams.class})
@RunLevel(4)
public class ServerWebSocketStreamsService
        implements ServerWebSocketStreams, PreDestroy {

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
    private final Map<String, Map<ReactiveStreamSubProtocol, ServerSubProtocol>> pathHandlers = new ConcurrentHashMap<>();
    private final Set<String> mappedPaths = new CopyOnWriteArraySet<>();

    private final TextMapPropagator textMapPropagator;
    private final Meter meter;
    private final Tracer tracer;
    private final ObservableLongUpDownCounter openConnections;
    private final AtomicLong connectionCounter = new AtomicLong();

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
        meter = openTelemetry.meterBuilder(ServerWebSocketStream.class.getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        tracer = openTelemetry.tracerBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        openConnections = meter.upDownCounterBuilder(OPEN_CONNECTIONS)
                .setUnit("{connection}")
                .buildWithCallback(observableLongMeasurement -> observableLongMeasurement.record(connectionCounter.get()));
    }

    @Override
    public <PUBLISH> Disposable publisher(String path, ServerWebSocketOptions options, Class<? super PUBLISH> publishType, Publisher<PUBLISH> publisher) throws IllegalArgumentException {
        return this.registerHandler(path, ReactiveStreamSubProtocol.publisher, new ServerPublisherSubProtocol<>(options, publishType, publisher, this));
    }

    @Override
    public <SUBSCRIBE> Disposable subscriber(String path, ServerWebSocketOptions options, Class<? super SUBSCRIBE> subscribeType, Function<Flux<SUBSCRIBE>, Disposable> subscriber) throws IllegalArgumentException {
        return registerHandler(path, ReactiveStreamSubProtocol.subscriber, new ServerSubscriberSubProtocol<>(options, subscribeType, subscriber, this));
    }

    @Override
    public <SUBSCRIBE, RESULT> Disposable subscriberWithResult(String path, ServerWebSocketOptions options, Class<? super SUBSCRIBE> subscribeType, Class<? super RESULT> resultType, Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscriberWithResultTransform) throws IllegalArgumentException {
        return this.registerHandler(path, ReactiveStreamSubProtocol.subscriberWithResult, new ServerSubscriberWithResultSubProtocol<>(options, subscribeType, resultType, subscriberWithResultTransform, this));
    }

    private Disposable registerHandler(
            String path,
            ReactiveStreamSubProtocol subProtocol,
            ServerSubProtocol serverSubProtocol
    ) {

        if (!path.contains("|") && !path.startsWith("/")) {
            path = "/" + path;
        }

        Map<ReactiveStreamSubProtocol, ServerSubProtocol> subProtocolHandlers = pathHandlers.computeIfAbsent(path, p -> new ConcurrentHashMap<>());

        if (subProtocolHandlers.containsKey(serverSubProtocol))
            throw new IllegalArgumentException("Handler for " + path + " and subprotocol " + serverSubProtocol + " already exists");

        // Only register once for each path
        if (mappedPaths.add(path)) {
            // Always register as URI template
            webSocketUpgradeHandler.getServerWebSocketContainer().addMapping("uri-template|" + path, webSocketCreator);
        }

        subProtocolHandlers.put(subProtocol, serverSubProtocol);

        return () -> {
            ServerSubProtocol removedSubProtocol = subProtocolHandlers.remove(subProtocol);
            if (removedSubProtocol != null) {
                removedSubProtocol.close();
            }
        };
    }

    MessageWriter<Object> getWriter(Class<?> type, ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse) {
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

    MessageReader<Object> getReader(Class<?> type, ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse) {
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

    AtomicLong getConnectionCounter() {
        return connectionCounter;
    }

    public LoggerContext getLoggerContext() {
        return loggerContext;
    }

    public Meter getMeter() {
        return meter;
    }

    public Tracer getTracer() {
        return tracer;
    }

    public ExecutorService getFlushingExecutors() {
        return flushingExecutors;
    }

    public ByteBufferPool getByteBufferPool() {
        return byteBufferPool;
    }

    class StreamWebSocketCreator
            implements WebSocketCreator {

        @Override
        public Object createWebSocket(ServerUpgradeRequest serverUpgradeRequest, ServerUpgradeResponse serverUpgradeResponse, Callback callback) {
            // Check if this is just a load balancer check
            String loadBalancing = serverUpgradeRequest.getHeaders().get("Xorcery-LoadBalancing");
            if (loadBalancing != null) {
                List<String> response = new ArrayList<>();
                for (String loadBalancingType : loadBalancing.split(",")) {
                    switch (loadBalancingType) {
                        case "connections": {
                            response.add("connections=" + connectionCounter.get());
                            break;
                        }
                    }
                }
                String loadBalancingResponse = String.join(",", response);
                serverUpgradeResponse.getHeaders().add("Xorcery-LoadBalancing", loadBalancingResponse);
                serverUpgradeResponse.setStatus(HttpStatus.NO_CONTENT_204);
                return null;
            }

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
                Map<ReactiveStreamSubProtocol, ServerSubProtocol> subProtocolHandlers;
                if (serverUpgradeRequest.getAttribute(PathSpec.class.getName()) instanceof UriTemplatePathSpec pathSpec) {
                    pathParameters = pathSpec.getPathParams(path);
                    String factoryPath = pathSpec.getDeclaration();
                    subProtocolHandlers = pathHandlers.get(factoryPath);
                } else {
                    subProtocolHandlers = pathHandlers.get(path);
                }
                if (subProtocolHandlers == null)
                    continue;
                ServerSubProtocol serverSubProtocol = subProtocolHandlers.get(requestSubProtocol);
                if (serverSubProtocol == null)
                    continue;

                Context context = textMapPropagator.extract(Context.current(), serverUpgradeRequest, jettyGetter);
                serverUpgradeResponse.setAcceptedSubProtocol(requestSubProtocol.name());
                serverUpgradeResponse.getHeaders().add("Aggregate", "maxBinaryMessageSize=" + webSocketUpgradeHandler.getServerWebSocketContainer().getMaxBinaryMessageSize());

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


                String clientHost = serverUpgradeRequest.getConnectionMetaData().getRemoteSocketAddress() instanceof InetSocketAddress isa
                        ? isa.getHostName()
                        : serverUpgradeRequest.getConnectionMetaData().getRemoteSocketAddress().toString();

                Session.Listener.AutoDemanding socketProtocolHandler = serverSubProtocol.createSubProtocolHandler(
                        serverUpgradeRequest,
                        serverUpgradeResponse,
                        clientHost,
                        path,
                        pathParameters,
                        clientMaxBinaryMessageSize,
                        context);
                return socketProtocolHandler;
            }

            // No protocols or handlers found matching this request
            serverUpgradeResponse.setStatus(HttpStatus.NOT_FOUND_404);
            return null;
        }
    }

    @Override
    public void preDestroy() {
        flushingExecutors.shutdown();
    }
}
