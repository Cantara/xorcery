package com.exoreaction.xorcery.service.domainevents;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.TopicSubscribers;
import com.exoreaction.xorcery.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.helpers.ClientPublisherGroupListener;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.xorcery.service.domainevents.api.entity.DomainEvents;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;
import java.util.function.Function;

@Service
@Named(DomainEventsService.SERVICE_TYPE)
@ContractsProvided({DomainEventPublisher.class, PreDestroy.class})
public class DomainEventsService
        implements DomainEventPublisher, PreDestroy {

    public static final String SERVICE_TYPE = "domainevents";

    private final ReactiveStreams reactiveStreams;
    private final DeploymentMetadata deploymentMetadata;
    private ServiceLocator serviceLocator;
    private final ServiceResourceObject resourceObject;
    private final UnicastEventHandler<WithMetadata<DomainEvents>> subscribers = new UnicastEventHandler<>();
    private final Disruptor<WithMetadata<DomainEvents>> disruptor;

    @Inject
    public DomainEventsService(ServiceResourceObjects serviceResourceObjects,
                               ReactiveStreams reactiveStreams,
                               ServiceLocator serviceLocator,
                               Configuration configuration) {

        this.reactiveStreams = reactiveStreams;
        this.serviceLocator = serviceLocator;
        this.resourceObject = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .websocket("domainevents", "ws/domainevents")
                .build();
        this.deploymentMetadata = new DomainEventMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();

        disruptor =
                new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("DomainEventsDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());
        disruptor.handleEventsWith(subscribers);
        disruptor.start();

        TopicSubscribers.addSubscriber(serviceLocator, new ClientPublisherGroupListener(resourceObject.getServiceIdentifier(), cfg -> new DomainEventsPublisher(), DomainEventsPublisher.class, null, reactiveStreams));

        resourceObject.getLinkByRel("domainevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new DomainEventsPublisher(), DomainEventsPublisher.class);
        });
        serviceResourceObjects.publish(resourceObject);
    }

    @Override
    public void preDestroy() {
        disruptor.shutdown();
    }

    public void publish(Metadata metadata, DomainEvents events) {
        disruptor.getRingBuffer().publishEvent((event, seq, m, e) ->
        {
            event.set(metadata.toBuilder().add(deploymentMetadata.context()).build(), e);
        }, metadata, events);
    }

    public class DomainEventsPublisher
            implements Flow.Publisher<WithMetadata<DomainEvents>> {

        public DomainEventsPublisher() {
            System.out.println("Connected to Neo4j");
        }

        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<DomainEvents>> subscriber) {
            subscriber.onSubscribe(subscribers.add(subscriber));
        }
    }
}
