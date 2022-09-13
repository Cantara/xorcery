package com.exoreaction.xorcery.service.domainevents;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.domainevents.api.aggregate.DomainEvents;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.service.reactivestreams.helper.ClientPublisherConductorListener;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

@Singleton
public class DomainEventsService
        implements DomainEventPublisher,
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
            context.register(DomainEventsService.class, DomainEventPublisher.class, ContainerLifecycleListener.class);
        }
    }

    private final ReactiveStreams reactiveStreams;
    private final DeploymentMetadata deploymentMetadata;
    private ServiceResourceObject resourceObject;
    private final UnicastEventHandler<WithResult<WithMetadata<DomainEvents>, Metadata>> subscribers = new UnicastEventHandler<>();
    private final Disruptor<WithResult<WithMetadata<DomainEvents>, Metadata>> disruptor;

    @Inject
    public DomainEventsService(ReactiveStreams reactiveStreams,
                               Conductor conductor,
                               @Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                               Configuration configuration) {
        this.reactiveStreams = reactiveStreams;
        this.resourceObject = resourceObject;
        this.deploymentMetadata = new DomainEventMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();

        disruptor =
                new Disruptor<>(WithResult::new, 4096, new NamedThreadFactory("DomainEventsDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());

        disruptor.handleEventsWith(subscribers);

        conductor.addConductorListener(new ClientPublisherConductorListener(resourceObject.serviceIdentifier(), cfg -> new DomainEventsPublisher(), DomainEventsPublisher.class, null, reactiveStreams));
    }

    @Override
    public void onStartup(Container container) {
        disruptor.start();
        resourceObject.getLinkByRel("domainevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new DomainEventsPublisher(), DomainEventsPublisher.class);
        });
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {
        disruptor.shutdown();
    }


    public CompletionStage<Metadata> publish(Metadata metadata, DomainEvents events) {
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        disruptor.getRingBuffer().publishEvent((event, seq, m, e, f) ->
        {
            event.set(new WithMetadata<>(deploymentMetadata.metadata()
                    .toBuilder().add(m).build(), e), f);
        }, metadata, events, future);

        return future.thenApply(md ->
        {
            // Return original metadata with this added on top
            metadata.metadata().setAll(md.metadata());
            return metadata;
        });
    }

    public class DomainEventsPublisher
            implements Flow.Publisher<WithResult<WithMetadata<DomainEvents>, Metadata>> {

        @Override
        public void subscribe(Flow.Subscriber<? super WithResult<WithMetadata<DomainEvents>, Metadata>> subscriber) {
            subscriber.onSubscribe(subscribers.add(subscriber));
        }
    }
}
