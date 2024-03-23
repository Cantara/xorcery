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
package com.exoreaction.xorcery.domainevents.publisher;

import com.exoreaction.xorcery.concurrent.CloseableSemaphore;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.domainevents.helpers.context.EventMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.util.SubscriberConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.yaml.snakeyaml.events.Event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

@Service(name = "domainevents")
@ContractsProvided({DomainEventPublisher.class, PreDestroy.class})
public class DomainEventsService
        implements DomainEventPublisher,
        Publisher<WithResult<WithMetadata<ArrayNode>, Metadata>>,
        PreDestroy {

    private final DeploymentMetadata deploymentMetadata;

    private Subscriber<? super WithResult<WithMetadata<ArrayNode>, Metadata>> subscriber;

    private final CloseableSemaphore requests = new CloseableSemaphore(0);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public DomainEventsService(ReactiveStreamsClient reactiveStreams,
                               Configuration configuration) {

        this.deploymentMetadata = new EventMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();
        DomainEventsConfiguration domainEventsConfiguration = new DomainEventsConfiguration(configuration.getConfiguration("domainevents"));
        SubscriberConfiguration subscriberConfiguration = domainEventsConfiguration.getSubscriberConfiguration();
        reactiveStreams.publish(subscriberConfiguration.getUri().orElse(null), subscriberConfiguration.getStream(),
                        subscriberConfiguration::getConfiguration,
                        this, DomainEventsService.class, new ClientConfiguration(domainEventsConfiguration.getPublisherConfiguration()))
                .exceptionally(t ->
                {
                    if (t instanceof CompletionException)
                        LogManager.getLogger(getClass()).error("Domain event publisher failed", t);
                    return null;
                });
    }

    @Override
    public void preDestroy() {
        requests.close();
        subscriber.onComplete();
    }

    @Override
    public void subscribe(Subscriber<? super WithResult<WithMetadata<ArrayNode>, Metadata>> subscriber) {
        this.subscriber = subscriber;
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {
                requests.release((int) n);
            }

            @Override
            public void cancel() {
                LogManager.getLogger(getClass()).warn("Domain event publisher cancelled");
            }
        });
    }

    public CompletableFuture<Metadata> publish(MetadataEvents commandEvents) {
        try {
            requests.tryAcquire(10, TimeUnit.SECONDS);
            CompletableFuture<Metadata> future = new CompletableFuture<>();
            ArrayNode json = objectMapper.valueToTree(commandEvents.getEvents());
            subscriber.onNext(new WithResult<>(new WithMetadata<>(commandEvents.getMetadata().toBuilder().add(deploymentMetadata.context()).build(), json), future));
            return future;
        } catch (InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
