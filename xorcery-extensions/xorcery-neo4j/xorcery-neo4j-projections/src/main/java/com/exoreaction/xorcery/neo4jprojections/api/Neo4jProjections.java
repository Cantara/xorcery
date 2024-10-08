package com.exoreaction.xorcery.neo4jprojections.api;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4jprojections.Neo4jProjectionHandler;
import com.exoreaction.xorcery.neo4jprojections.Neo4jProjectionUpdates;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.util.function.BiFunction;
import java.util.function.Function;

@Service
public class Neo4jProjections {
    private final Neo4jProjectionHandler projectionHandler;
    private final Neo4jProjectionUpdates projectionUpdates;

    @Inject
    public Neo4jProjections(
            Neo4jProjectionHandler neo4jProjectionHandler,
            @Optional Neo4jProjectionUpdates projectionUpdates
    ) {
        this.projectionHandler = neo4jProjectionHandler;
        this.projectionUpdates = projectionUpdates;
    }

    /**
     * To be used with {@link Flux#transformDeferred(Function)}.
     * Subscribers must place {@link ProjectionStreamContext#projectionId} into their {@link CoreSubscriber#currentContext()}.
     * The projection will add {@link ProjectionStreamContext#projectionPosition} to the context if the projection already exists.
     * @return
     */
    public BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> projection() {
        if (projectionUpdates == null)
        {
            return projectionHandler;
        } else
        {
            return projectionHandler.andThen(publisher -> Flux.from(publisher).transformDeferredContextual(projectionUpdates));
        }
    }
}
