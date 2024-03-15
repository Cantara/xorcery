package com.exoreaction.xorcery.reactivestreams.server.reactor;


import com.exoreaction.xorcery.lang.Classes;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service(name = "reactivestreams.server.reactor")
@ContractsProvided({WebSocketStreamsServer.class})
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
    private final JettyWebSocketServerContainer container;
    private final Logger logger;

    protected final ByteBufferPool byteBufferPool;


    // Path -> Content type -> publisher
    private final Map<String, Map<String, SocketPublisher>> publishers = new ConcurrentHashMap<>();

    // Path -> Content type -> subscriber
    private final Map<String, Map<String, SocketSubscriber>> subscribers = new ConcurrentHashMap<>();

    private final TextMapPropagator textMapPropagator;
    private final Meter meter;
    private final Tracer tracer;

    private final JettyWebSocketCreator publisherWebSocketCreator = new PublisherWebSocketCreator();
    private final JettyWebSocketCreator subscriberWebSocketCreator = new SubscriberWebSocketCreator();

    @Inject
    public WebSocketStreamsServerService(
            MessageWorkers messageWorkers,
            ServletContextHandler servletContextHandler,
            OpenTelemetry openTelemetry,
            Logger logger) {
        this.messageWorkers = messageWorkers;

        container = JettyWebSocketServerContainer.getContainer(servletContextHandler.getServletContext());
        this.logger = logger;

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
    public <PUBLISH> Disposable publisher(String path, String contentType, Class<PUBLISH> publishType, Publisher<PUBLISH> publisher) throws IllegalArgumentException {

        if (!path.contains("|") && !path.startsWith("/")) {
            path = "/" + path;
        }

        MessageWriter<Object> itemWriter = Optional.ofNullable(messageWorkers.newWriter(publishType, publishType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + publishType.getTypeName() + "(" + contentType + ")"));

        Map<String, SocketPublisher> contentTypePublishers = publishers.get(path);
        if (contentTypePublishers == null) {
            container.addMapping(path, publisherWebSocketCreator);
            contentTypePublishers = new ConcurrentHashMap<>();
            publishers.put(path, contentTypePublishers);
        }

        if (contentTypePublishers.containsKey(contentType))
            throw new IllegalArgumentException("Publisher for " + path + " and content type " + contentType + " already exists");
        contentTypePublishers.put(contentType, new SocketPublisher(null, itemWriter, flux -> Flux.from(publisher)));

        String finalPath = path;
        return () -> publishers.get(finalPath).remove(contentType);
    }

    @Override
    public <SUBSCRIBE> Disposable subscriber(String path, String contentType, Class<SUBSCRIBE> subscribeType, Function<Flux<SUBSCRIBE>, Publisher<SUBSCRIBE>> appendSubscriber) throws IllegalArgumentException {
        if (!path.contains("|") && !path.startsWith("/")) {
            path = "/" + path;
        }

        MessageReader<SUBSCRIBE> itemReader = Optional.ofNullable(messageWorkers.<SUBSCRIBE>newReader(subscribeType, subscribeType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + subscribeType.getTypeName() + "(" + contentType + ")"));

        Map<String, SocketSubscriber> contentTypeSubscribers = subscribers.get(path);
        if (contentTypeSubscribers == null) {
            container.addMapping(path, subscriberWebSocketCreator);
            contentTypeSubscribers = new ConcurrentHashMap<>();
            subscribers.put(path, contentTypeSubscribers);
        }

        if (contentTypeSubscribers.containsKey(contentType))
            throw new IllegalArgumentException("Subscriber for " + path + " and content type " + contentType + " already exists");
        contentTypeSubscribers.put(contentType, new SocketSubscriber(itemReader, null, (Function) appendSubscriber));

        String finalPath = path;
        return () -> publishers.get(finalPath).remove(contentType);
    }

    @Override
    public <SUBSCRIBE, RESULT> Disposable subscriberWithResult(String path, String contentType, Class<SUBSCRIBE> subscribeType, Class<RESULT> resultType, Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeAndReturnResult) throws IllegalArgumentException {
        if (!path.contains("|") && !path.startsWith("/")) {
            path = "/" + path;
        }

        MessageReader<SUBSCRIBE> itemReader = Optional.ofNullable(messageWorkers.<SUBSCRIBE>newReader(subscribeType, subscribeType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageReader for " + subscribeType.getTypeName() + "(" + contentType + ")"));
        MessageWriter<RESULT> itemWriter = Optional.ofNullable(messageWorkers.<RESULT>newWriter(resultType, resultType, contentType))
                .orElseThrow(() -> new IllegalStateException("Could not find MessageWriter for " + resultType.getTypeName() + "(" + contentType + ")"));

        Map<String, SocketSubscriber> contentTypeSubscribers = subscribers.get(path);
        if (contentTypeSubscribers == null) {
            container.addMapping(path, subscriberWebSocketCreator);
            contentTypeSubscribers = new ConcurrentHashMap<>();
            subscribers.put(path, contentTypeSubscribers);
        }

        if (contentTypeSubscribers.containsKey(contentType))
            throw new IllegalArgumentException("Subscriber for " + path + " and content type " + contentType + " already exists");
        contentTypeSubscribers.put(contentType, new SocketSubscriber(itemReader, itemWriter, (Function) subscribeAndReturnResult));

        String finalPath = path;
        return () -> publishers.get(finalPath).remove(contentType);

    }

    record SocketPublisher(MessageReader<?> itemReader, MessageWriter<?> itemWriter,
                           Function<Flux<Object>, Publisher<Object>> appendPublisher) {
    }

    record SocketSubscriber(MessageReader<?> itemReader, MessageWriter<?> itemWriter,
                            Function<Flux<Object>, Publisher<Object>> appendSubscriber) {
    }

    class PublisherWebSocketCreator
            implements JettyWebSocketCreator {
        @Override
        public Object createWebSocket(JettyServerUpgradeRequest jettyServerUpgradeRequest, JettyServerUpgradeResponse jettyServerUpgradeResponse) {
            try {
                String path = jettyServerUpgradeRequest.getRequestPath();
                String contentType = jettyServerUpgradeRequest.getHeader(HttpHeader.CONTENT_TYPE.asString());

                Map<String, WebSocketStreamsServerService.SocketPublisher> contentTypePublishers = publishers.get(path);
                if (contentTypePublishers == null) {
                    jettyServerUpgradeResponse.sendError(HttpStatus.NOT_FOUND_404, "Not found");
                    return null;
                }

                WebSocketStreamsServerService.SocketPublisher publisherFactory;
                if (contentType == null) {
                    if (contentTypePublishers.size() == 1) {
                        publisherFactory = contentTypePublishers.get(contentType);
                    } else {
                        publisherFactory = null;
                        jettyServerUpgradeResponse.sendError(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415, "Media type not specified");
                        return null;
                    }
                } else {

                    publisherFactory = contentTypePublishers.get(contentType);
                    if (publisherFactory == null) {
                        jettyServerUpgradeResponse.sendError(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415, "Media type " + contentType + " not supported");
                        return null;
                    }
                }

                Map<String, String> publisherParameters = new HashMap<>();
                jettyServerUpgradeRequest.getParameterMap().forEach((k, v) -> publisherParameters.put(k, v.get(0)));
                if (jettyServerUpgradeRequest.getServletAttribute("org.eclipse.jetty.http.pathmap.PathSpec") instanceof UriTemplatePathSpec pathSpec) {
                    Map<String, String> pathParams = pathSpec.getPathParams(jettyServerUpgradeRequest.getRequestPath());
                    publisherParameters.putAll(pathParams);
                }

                Context context = textMapPropagator.extract(Context.current(), jettyServerUpgradeRequest, jettyGetter);

                Function<Flux<Object>, Publisher<Object>> appendPublisher = publisherFactory.appendPublisher();

                return new ServerWebSocketStream<>(
                        path,
                        publisherParameters,
                        (MessageWriter<Object>) publisherFactory.itemWriter(),
                        (MessageReader<Object>) publisherFactory.itemReader(),
                        appendPublisher,
                        byteBufferPool,
                        logger,
                        tracer,
                        meter,
                        context
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    class SubscriberWebSocketCreator
            implements JettyWebSocketCreator {
        @Override
        public Object createWebSocket(JettyServerUpgradeRequest jettyServerUpgradeRequest, JettyServerUpgradeResponse jettyServerUpgradeResponse) {
            try {
                Map<String, String> subscriberParameters = new HashMap<>();

                Map<String, WebSocketStreamsServerService.SocketSubscriber> contentTypeSubscriberFactories;
                String path = jettyServerUpgradeRequest.getRequestPath();
                if (jettyServerUpgradeRequest.getServletAttribute("org.eclipse.jetty.http.pathmap.PathSpec") instanceof UriTemplatePathSpec pathSpec) {
                    String factoryPath = "uri-template|" + pathSpec.getDeclaration();
                    contentTypeSubscriberFactories = subscribers.get(factoryPath);
                    Map<String, String> pathParams = pathSpec.getPathParams(path);
                    subscriberParameters.putAll(pathParams);
                } else {
                    contentTypeSubscriberFactories = subscribers.get(path);
                }

                if (contentTypeSubscriberFactories == null) {
                    jettyServerUpgradeResponse.sendError(HttpStatus.NOT_FOUND_404, "Not found");
                    return null;
                }

                String contentType = jettyServerUpgradeRequest.getHeader(HttpHeader.CONTENT_TYPE.asString());
                WebSocketStreamsServerService.SocketSubscriber subscriberFactory = contentTypeSubscriberFactories.get(contentType);
                if (subscriberFactory == null) {
                    jettyServerUpgradeResponse.sendError(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415, "Media type " + contentType + " not supported");
                    return null;
                }

                jettyServerUpgradeRequest.getParameterMap().forEach((k, v) -> subscriberParameters.put(k, v.get(0)));

                Context context = textMapPropagator.extract(Context.current(), jettyServerUpgradeRequest, jettyGetter);

                // TODO Replace with subscriber version
                Function<Flux<Object>, Publisher<Object>> appendSubscriber = subscriberFactory.appendSubscriber();
                return new ServerWebSocketStream<>(
                        path,
                        subscriberParameters,
                        (MessageWriter<Object>) subscriberFactory.itemWriter(),
                        (MessageReader<Object>) subscriberFactory.itemReader(),
                        appendSubscriber,
                        byteBufferPool,
                        logger,
                        tracer,
                        meter,
                        context
                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
