package com.exoreaction.xorcery.service.domainevents;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventPublisher;
import com.exoreaction.xorcery.service.domainevents.api.entity.DomainEvents;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

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
                .configuration(configuration)
                .build();
        reactiveStreams.publish(configuration.getString("domainevents.subscriber.authority").orElseThrow(), configuration.getString("domainevents.subscriber.stream").orElseThrow(),
                        () -> configuration.getConfiguration("domainevents.subscriber.configuration"),
                        this, DomainEventsService.class,configuration.getConfiguration("domainevents.publisher.configuration"))
                .exceptionally(t ->
                {
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
