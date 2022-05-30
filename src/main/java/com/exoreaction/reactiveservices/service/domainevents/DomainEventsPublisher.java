package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.disruptor.DeploymentMetadata;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.json.JsonObject;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class DomainEventsPublisher
        implements DomainEventPublisher,
        ReactiveEventStreams.Publisher<EventWithResult<DomainEvents, Metadata>>,
        ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "domainevents";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.websocket("domainevents", "ws/domainevents");
        }

        @Override
        protected void configure() {
            context.register(DomainEventsPublisher.class, DomainEventPublisher.class, ContainerLifecycleListener.class);
        }
    }

    private final ReactiveStreams reactiveStreams;
    private final DeploymentMetadata deploymentMetadata;
    private ServiceResourceObject resourceObject;
    private final List<EventSink<Event<EventWithResult<DomainEvents, Metadata>>>> subscribers = new CopyOnWriteArrayList<>();
    private final Disruptor<DomainEventHolder> disruptor;

    @Inject
    public DomainEventsPublisher(ReactiveStreams reactiveStreams, Server server,
                                 @Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                                 Configuration configuration) {
        this.reactiveStreams = reactiveStreams;
        this.resourceObject = resourceObject;
        this.deploymentMetadata = new DeploymentMetadata(configuration);

        disruptor =
                new Disruptor<>(DomainEventHolder::new, 4096, new NamedThreadFactory("DomainEventsDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());

        disruptor.handleEventsWith(new UnicastEventHandler<>(subscribers));
    }

    @Override
    public void onStartup(Container container) {
        disruptor.start();
        resourceObject.getLinkByRel("domainevents").ifPresent(link ->
        {
            reactiveStreams.publish(resourceObject.serviceIdentifier(), link, this);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        disruptor.shutdown();
    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<EventWithResult<DomainEvents, Metadata>> subscriber, JsonObject parameters) {
        final AtomicReference<EventSink<Event<EventWithResult<DomainEvents, Metadata>>>> handler = new AtomicReference<>();
        handler.set(subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                // Ignore for now
            }

            @Override
            public void cancel() {
                subscribers.remove(handler.get());
            }
        }));
        subscribers.add(handler.get());
    }

    public CompletionStage<Metadata> publish(Metadata metadata, DomainEvents events) {
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        disruptor.getRingBuffer().publishEvent((event, seq, e, f) ->
        {
            event.metadata = deploymentMetadata.metadata()
                    .toBuilder().add(metadata).build();
            event.event = new EventWithResult<>(e, f);
        }, events, future);

        return future;
    }
}
