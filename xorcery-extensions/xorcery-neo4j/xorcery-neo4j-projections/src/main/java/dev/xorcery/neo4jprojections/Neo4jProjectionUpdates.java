package dev.xorcery.neo4jprojections;

import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.neo4jprojections.api.ProjectionStreamContext;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Signal;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

@Service(name = "neo4jprojections.updates")
public class Neo4jProjectionUpdates
        implements Publisher<Metadata>,
        BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>>,
        PreDestroy {
    private final Map<String, SinkFlux> sinkFluxes = new ConcurrentHashMap<>();

    @Inject
    public Neo4jProjectionUpdates() {
    }

    @Override
    public void subscribe(Subscriber<? super Metadata> s) {
        if (s instanceof CoreSubscriber<? super Metadata> coreSubscriber) {
            new ContextViewElement(coreSubscriber.currentContext())
                    .getString(ProjectionStreamContext.projectionId)
                    .ifPresentOrElse(pid ->
                    {
                        sinkFluxes.computeIfAbsent(pid, Neo4jProjectionUpdates::createSinkFlux).flux.subscribe(s);
                    }, () ->
                    {
                        s.onError(new IllegalArgumentException("Must provide projectionId context key"));
                    });
        } else {
            s.onError(new IllegalArgumentException("Must implement CoreSubscriber"));
        }
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> projectionFlux, ContextView contextView) {

        return new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId).map(pid ->
        {
            SinkFlux sinkFlux = sinkFluxes.computeIfAbsent(pid, Neo4jProjectionUpdates::createSinkFlux);
            return projectionFlux.doOnEach(this.onEach(sinkFlux.sink()));
        }).orElse(projectionFlux);
    }

    private Consumer<? super Signal<MetadataEvents>> onEach(Sinks.Many<Metadata> sink) {
        return signal ->
        {
            switch (requireNonNull(signal.getType())) {
                case CANCEL -> {
                    sink.tryEmitComplete();
                }
                case ON_NEXT -> {
                    sink.tryEmitNext(requireNonNull(signal.get()).metadata());
                }
                case ON_ERROR -> {
                    sink.tryEmitError(requireNonNull(signal.getThrowable()));
                }
                case ON_COMPLETE -> {
                    sink.tryEmitComplete();
                }
                default -> {
                }
            };
        };
    }

    private static SinkFlux createSinkFlux(String projectionId) {
        Sinks.Many<Metadata> sink = Sinks.many().replay().limit(1);
        Flux<Metadata> projectionUpdatesFlux = sink.asFlux();
        return new SinkFlux(sink, projectionUpdatesFlux);
    }

    @Override
    public void preDestroy() {
        sinkFluxes.values().forEach(sinkFlux -> sinkFlux.sink().tryEmitComplete());
    }

    record SinkFlux(Sinks.Many<Metadata> sink, Flux<Metadata> flux) {
    }
}
