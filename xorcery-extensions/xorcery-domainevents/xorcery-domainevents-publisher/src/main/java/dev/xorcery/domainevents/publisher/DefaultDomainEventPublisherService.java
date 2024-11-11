package dev.xorcery.domainevents.publisher;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.publisher.api.DomainEventPublisher;
import dev.xorcery.domainevents.publisher.spi.EventProjectionProvider;
import dev.xorcery.domainevents.publisher.spi.EventPublisherProvider;
import dev.xorcery.metadata.DeploymentMetadata;
import dev.xorcery.metadata.Metadata;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service(name="domainevents.eventpublisher.default")
@ContractsProvided({DomainEventPublisher.class})
public class DefaultDomainEventPublisherService
        implements DomainEventPublisher,
        PreDestroy {

    private final DeploymentMetadata deploymentMetadata;

    private final Sinks.Many<MetadataEvents> sink;
    private final Disposable subscribeDisposable;
    private final CountDownLatch completeLatch = new CountDownLatch(1);
    private final Queue<CompletableFuture<Metadata>> requestQueue = new ArrayBlockingQueue<>(4096);

    @Inject
    public DefaultDomainEventPublisherService(
            Configuration configuration,
            EventPublisherProvider eventPublisherProvider,
            EventProjectionProvider eventProjectionProvider) {
        this.deploymentMetadata = new CommandMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();
        sink = Sinks.many().multicast().onBackpressureBuffer();

        subscribeDisposable = sink.asFlux()
                .transformDeferredContextual(eventPublisherProvider)
                .transformDeferredContextual(eventProjectionProvider)
                .doOnNext(metadata -> requestQueue.remove().complete(metadata))
                .doOnError(throwable ->
                {
                    for (CompletableFuture<Metadata> future : requestQueue) {
                        future.completeExceptionally(throwable);
                    }
                })
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3)).maxBackoff(Duration.ofSeconds(30)))
                .doOnComplete(() ->
                {
                    for (CompletableFuture<Metadata> future : requestQueue) {
                        future.completeExceptionally(new IllegalStateException("Shutting down"));
                    }
                    completeLatch.countDown();
                })
                .subscribe();
    }

    @Override
    public CompletableFuture<Metadata> publish(MetadataEvents metadataEvents) {
        metadataEvents.metadata().toBuilder().add(deploymentMetadata.context());
        CompletableFuture<Metadata> future = new CompletableFuture<>();
        if (requestQueue.offer(future)) {
            Sinks.EmitResult emitResult = sink.tryEmitNext(metadataEvents);
            if (emitResult.isFailure()) {
                return CompletableFuture.failedFuture(new ProcessingException("Could not publish events"));
/* TODO Implement this switch for better error messages
                return switch (emitResult)
                {
                    case OK -> null;
                    case FAIL_TERMINATED -> null;
                    case FAIL_OVERFLOW -> null;
                    case FAIL_CANCELLED -> null;
                    case FAIL_NON_SERIALIZED -> null;
                    case FAIL_ZERO_SUBSCRIBER -> null;
                }
*/
            }
            return future;
        } else {
            return CompletableFuture.failedFuture(new ProcessingException("Could not publish events, queue is full"));
        }

    }

    @Override
    public void preDestroy() {
        sink.tryEmitComplete();
        try {
            completeLatch.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

/*
TODO Should this be done by the context?
    private CommandMetadata addMetadata(CommandMetadata metadata, Command command) {
        return new CommandMetadata.Builder(metadata.context())
                .timestamp(System.currentTimeMillis())
                .correlationId(UUIDs.newId())
                .domain("todo")
                .commandName(command.getClass().getSimpleName())
                .build();
    }
*/

}
