package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;

@Service
public class Neo4jProjections {
    private final Provider<Neo4jProjectionHandler> projectionHandlerProvider;

    @Inject
    public Neo4jProjections(
            Provider<Neo4jProjectionHandler> projectionHandlerProvider
    ) {
        this.projectionHandlerProvider = projectionHandlerProvider;
    }

    public BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> projection() {
        return projectionHandlerProvider.get();
    }
}
