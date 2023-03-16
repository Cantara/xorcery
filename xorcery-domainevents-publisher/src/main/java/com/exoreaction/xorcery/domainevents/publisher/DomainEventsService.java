package com.exoreaction.xorcery.domainevents.publisher;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.domainevents.api.DomainEvents;
import com.exoreaction.xorcery.domainevents.helpers.context.DomainEventMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.util.SubscriberConfiguration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

@Service(name = "domainevents")
@ContractsProvided({DomainEventPublisher.class, PreDestroy.class})
public class DomainEventsService
        implements DomainEventPublisher,
        Flow.Publisher<WithMetadata<DomainEvents>>,
        PreDestroy {

    private final DeploymentMetadata deploymentMetadata;

    private Flow.Subscriber<? super WithMetadata<DomainEvents>> subscriber;

    @Inject
    public DomainEventsService(ReactiveStreamsClient reactiveStreams,
                               Configuration configuration) {

        this.deploymentMetadata = new DomainEventMetadata.Builder(new Metadata.Builder())
                .configuration(new InstanceConfiguration(configuration.getConfiguration("instance")))
                .build();
        DomainEventsConfiguration domainEventsConfiguration = new DomainEventsConfiguration(configuration.getConfiguration("domainevents"));
        SubscriberConfiguration subscriberConfiguration = domainEventsConfiguration.getSubscriberConfiguration();
        reactiveStreams.publish(subscriberConfiguration.getAuthority(), subscriberConfiguration.getStream(),
                        subscriberConfiguration::getConfiguration,
                        this, DomainEventsService.class, domainEventsConfiguration.getPublisherConfiguration())
                .exceptionally(t ->
                {
                    if (t instanceof CompletionException)
                        LogManager.getLogger(getClass()).error("Domain event publisher failed", t);
                    return null;
                });
    }

    @Override
    public void preDestroy() {
        subscriber.onComplete();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<DomainEvents>> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {
                LogManager.getLogger(getClass()).warn("Domain event publisher cancelled");
            }
        });
    }

    public void publish(Metadata metadata, DomainEvents events) {
        if (subscriber != null)
            subscriber.onNext(new WithMetadata<>(metadata.toBuilder().add(deploymentMetadata.context()).build(), events));
    }
}
