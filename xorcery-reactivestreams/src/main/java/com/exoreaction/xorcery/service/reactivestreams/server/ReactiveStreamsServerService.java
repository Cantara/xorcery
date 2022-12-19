package com.exoreaction.xorcery.service.reactivestreams.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.ReactiveStreamsAbstractService;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import jakarta.inject.Inject;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@ContractsProvided({ReactiveStreamsServer.class})
@RunLevel(6)
public class ReactiveStreamsServerService
        extends ReactiveStreamsAbstractService
        implements ReactiveStreamsServer {
    private final Map<String, Supplier<Object>> publisherEndpointFactories = new ConcurrentHashMap<>();
    private final Map<String, Supplier<Object>> subscriberEndpointFactories = new ConcurrentHashMap<>();

    @Inject
    public ReactiveStreamsServerService(Configuration configuration,
                                        MessageWorkers messageWorkers,
                                        ServletContextHandler servletContextHandler) {
        super(messageWorkers);

        PublishersReactiveStreamsServlet publishersServlet = new PublishersReactiveStreamsServlet(configuration, streamName ->
        {
            return Optional.ofNullable(publisherEndpointFactories.get(streamName)).map(Supplier::get).orElse(null);
        });
        servletContextHandler.addServlet(new ServletHolder(publishersServlet), "/streams/publishers/*");

        SubscribersReactiveStreamsServlet subscribersServlet = new SubscribersReactiveStreamsServlet(configuration, streamName ->
        {
            return Optional.ofNullable(subscriberEndpointFactories.get(streamName)).map(Supplier::get).orElse(null);
        });
        servletContextHandler.addServlet(new ServletHolder(subscribersServlet), "/streams/subscribers/*");

    }

    public CompletableFuture<Void> publisher(String streamName,
                                             final Function<Configuration, ? extends Flow.Publisher<?>> publisherFactory,
                                             Class<? extends Flow.Publisher<?>> publisherType) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscriptions to this publisher. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            publisherEndpointFactories.remove(streamName);
        });

        Type type = resolveActualTypeArgs(publisherType, Flow.Publisher.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageWriter<Object> eventWriter = getWriter(eventType);
        MessageReader<Object> resultReader = resultType.map(this::getReader).orElse(null);

        Function<Configuration, Flow.Publisher<Object>> wrappedPublisherFactory = (config) -> new ReactiveStreamsAbstractService.PublisherTracker((Flow.Publisher<Object>) publisherFactory.apply(config));

        publisherEndpointFactories.put(streamName, () ->
        {
            return new PublisherReactiveStream(streamName, wrappedPublisherFactory, eventWriter, resultReader, objectMapper, byteBufferPool);
        });

        return result;
    }

    public CompletableFuture<Void> subscriber(String streamName,
                                              Function<Configuration, Flow.Subscriber<?>> subscriberFactory,
                                              Class<? extends Flow.Subscriber<?>> subscriberType) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        // TODO Track the subscriptions to this publisher. Need to ensure they are cancelled on shutdown and cancel of the future
        result.whenComplete((r, t) ->
        {
            publisherEndpointFactories.remove(streamName);
        });

        Type type = resolveActualTypeArgs(subscriberType, Flow.Subscriber.class)[0];
        Type eventType = getEventType(type);
        Optional<Type> resultType = getResultType(type);
        MessageReader<Object> eventReader = getReader(eventType);
        MessageWriter<Object> resultWriter = resultType.map(this::getWriter).orElse(null);

        Function<Configuration, Flow.Subscriber<Object>> wrappedSubscriberFactory = (config) -> new ReactiveStreamsAbstractService.SubscriberTracker((Flow.Subscriber<Object>) subscriberFactory.apply(config));

        subscriberEndpointFactories.put(streamName, () ->
        {
            return new SubscriberReactiveStream(streamName, wrappedSubscriberFactory, eventReader, resultWriter, objectMapper, byteBufferPool, timer);
        });

        return result;
    }


    public Function<Configuration, Flow.Subscriber<?>> getSubscriberFactory(String streamName) {
        return null;
    }

    public Function<Configuration, ? extends Flow.Publisher<?>> getPublisherFactory(String streamName) {
        return null;
    }
}
