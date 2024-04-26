package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Service(name = "neo4jprojections.updates")
public class Neo4jProjectionUpdates
        implements Publisher<Metadata>,
        Function<MetadataEvents, MetadataEvents>,
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
    public MetadataEvents apply(MetadataEvents metadataEvents) {
        metadataEvents.getMetadata().getString("projectionId")
                .ifPresent(id -> sinkFluxes.computeIfAbsent(id, Neo4jProjectionUpdates::createSinkFlux).sink.tryEmitNext(metadataEvents.getMetadata()));

        return metadataEvents;
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
