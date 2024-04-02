package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service
public class Neo4jProjections {
    private final Neo4jProjectionHandler projectionHandler;
    private final Neo4jProjectionUpdates projectionUpdates;

    @Inject
    public Neo4jProjections(
            Neo4jProjectionHandler neo4jProjectionHandler,
            Neo4jProjectionUpdates projectionUpdates
    ) {
        this.projectionHandler = neo4jProjectionHandler;
        this.projectionUpdates = projectionUpdates;
    }

    public BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> projection() {
        return projectionHandler.andThen(publisher -> Flux.from(publisher).map(projectionUpdates));
    }
}
