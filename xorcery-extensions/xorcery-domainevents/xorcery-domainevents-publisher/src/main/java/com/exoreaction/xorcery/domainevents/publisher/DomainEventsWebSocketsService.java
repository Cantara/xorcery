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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.domainevents.helpers.context.EventMetadata;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketOptions;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreamContext;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientWebSocketStreams;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service(name = "domainevents")
@ContractsProvided({DomainEventPublisher.class})
public class DomainEventsWebSocketsService
        implements DomainEventPublisher,
        PreDestroy {

    private final DeploymentMetadata deploymentMetadata;
    private final Disposable subscribeDisposable;
    private final Sinks.Many<MetadataEvents> sink;
    private final CountDownLatch completeLatch = new CountDownLatch(1);
    private final Queue<CompletableFuture<Metadata>> requestQueue = new ArrayBlockingQueue<>(4096);

    @Inject
    public DomainEventsWebSocketsService(ClientWebSocketStreams clientWebSocketStreams,
                                         Configuration configuration) {

        // TODO This needs to updated to latest DomainEventMetadata values
        this.deploymentMetadata = new EventMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();
        DomainEventsConfiguration domainEventsConfiguration = new DomainEventsConfiguration(configuration.getConfiguration("domainevents"));
        sink = Sinks.many().multicast().onBackpressureBuffer();
        Optional<URI> eventStoreUri = configuration.getURI("domainevents.eventstore");
        Optional<URI> projectionsUri = configuration.getURI("domainevents.projections");
        Flux<Metadata> projections = eventStoreUri.map(uri ->
        {
            // Publish events to EventStore
            Flux<Metadata> result = sink.asFlux()
                    .transform(clientWebSocketStreams.publishWithResult(ClientWebSocketOptions.instance(), MetadataEvents.class, Metadata.class))
                    .contextWrite(Context.of(ClientWebSocketStreamContext.serverUri.name(),uri));

            // Then wait for projection
            return result
                    .transform(clientWebSocketStreams.publishWithResult(ClientWebSocketOptions.instance(), Metadata.class, Metadata.class))
                    .contextWrite(Context.of(ClientWebSocketStreamContext.serverUri.name(),projectionsUri.orElseThrow()));
        }).orElseGet(() ->
        {
            // Publish events directly to projections
            return sink.asFlux()
                    .transform(clientWebSocketStreams.publishWithResult(ClientWebSocketOptions.instance(), MetadataEvents.class, Metadata.class))
                    .contextWrite(Context.of(ClientWebSocketStreamContext.serverUri.name(),projectionsUri.orElseThrow()));
        });

        subscribeDisposable = projections
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3)).maxBackoff(Duration.ofSeconds(30)))
                .doOnNext(metadata -> requestQueue.remove().complete(metadata))
                .doOnError(throwable ->
                {
                    for (CompletableFuture<Metadata> future : requestQueue) {
                        future.completeExceptionally(throwable);
                    }
                })
                .doOnComplete(() ->
                {
                    for (CompletableFuture<Metadata> future : requestQueue) {
                        future.completeExceptionally(new ServerShutdownStreamException(1001, "Shutting down"));
                    }
                })
                .doOnComplete(completeLatch::countDown)
                .subscribe();
    }

    @Override
    public void preDestroy() {
        sink.tryEmitComplete();
        try {
            completeLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // Ignore
        }
        subscribeDisposable.dispose();
    }

    public CompletableFuture<Metadata> publish(MetadataEvents metadataEvents) {
        metadataEvents.metadata().toBuilder().add(deploymentMetadata.context());
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        if (requestQueue.offer(future)) {
            if (sink.tryEmitNext(metadataEvents).isFailure()) {
                return CompletableFuture.failedFuture(new ProcessingException("Could not publish events"));
            }
            return future;
        } else {
            return CompletableFuture.failedFuture(new ProcessingException("Could not publish events, queue is full"));
        }
    }
}
