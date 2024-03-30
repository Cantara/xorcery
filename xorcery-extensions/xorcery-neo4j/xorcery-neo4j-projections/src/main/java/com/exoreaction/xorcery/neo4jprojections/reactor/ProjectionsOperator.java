package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Service
public class ProjectionsOperator
        implements BiFunction<Flux<List<MetadataEvents>>, ContextView, Publisher<List<MetadataEvents>>> {
    private final GraphDatabase database;
    private final Provider<ProjectionHandler.Factory> handlerProvider;
    private final Logger logger;

    @Inject
    public ProjectionsOperator(GraphDatabase database, Provider<ProjectionHandler.Factory> handlerProvider, Logger logger) {
        this.database = database;
        this.handlerProvider = handlerProvider;
        this.logger = logger;
    }

    @Override
    public Publisher<List<MetadataEvents>> apply(Flux<List<MetadataEvents>> flux, ContextView contextView) {
        String projection = contextView.get("projection").toString();
        // TODO Lookup current revision and set as context
        Optional<ProjectionModel> currentProjection = getCurrentProjection(projection);
        return flux.contextWrite(currentProjection.map(p -> Context.of("revision", p.getRevision())).orElse(Context.empty()))
                .handle(handlerProvider.get().create(projection, currentProjection));
    }

    public Optional<ProjectionModel> getCurrentProjection(String projectionId) {
        // Check if we already have written data for this projection before
        return database.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.version, Projection.revision)
                .first(row -> row.toModel(ProjectionModel::new, Projection.version, Projection.revision)).handle((model, exception) ->
                {
                    if (exception != null) {
                        logger.error("Error looking up existing projection details", exception);
                        return Optional.<ProjectionModel>empty();
                    }
                    return model;
                }).toCompletableFuture().join();
    }

}
