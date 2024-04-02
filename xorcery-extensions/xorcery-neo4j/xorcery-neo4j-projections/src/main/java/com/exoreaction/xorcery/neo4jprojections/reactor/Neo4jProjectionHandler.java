package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.reactivestreams.disruptor.SmartBatching;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@Service
public class Neo4jProjectionHandler
        implements BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> {

    private final GraphDatabaseService database;
    private final Logger logger;
    private final List<Neo4jEventProjection> projections = new ArrayList<>();

    private final DisruptorConfiguration configuration;

    @Inject
    public Neo4jProjectionHandler(
            GraphDatabase database,
            IterableProvider<Neo4jEventProjection> projections,
            Neo4jProjectionUpdates neo4jProjectionUpdates,
            Configuration configuration,
            Logger logger
    ) {
        this.database = database.getGraphDatabaseService();
        this.logger = logger;
        projections.forEach(this.projections::add);
        this.configuration = new DisruptorConfiguration(configuration.getConfiguration("neo4jprojections.disruptor"));
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {

        String projection = contextView.get(ProjectionStreamContext.projectionId.name()).toString();
        Optional<ProjectionModel> currentProjection = getCurrentProjection(projection);
        return currentProjection.flatMap(ProjectionModel::getProjectionPosition)
                .map(p -> new SmartBatching<>(this.configuration, new Handler(p)).apply(metadataEventsFlux.contextWrite(Context.of(Projection.projectionPosition.name(), p)), contextView))
                .orElseGet(() ->
                {
                    // Create projection in Neo4j
                    try (Transaction tx = database.beginTx()) {
                        Map<String, Object> createParameters = new HashMap<>();
                        createParameters.put(Projection.projectionId.name(), contextView.get(ProjectionStreamContext.projectionId.name()));
                        Map<String, String> props = new HashMap<>();
                        contextView.forEach((k, v) ->
                        {
                            if (!k.equals(ProjectionStreamContext.projectionId.name())) {
                                props.put(k.toString(), v.toString());
                            }
                        });
                        createParameters.put("props", props);
                        tx.execute("""
                                CREATE (projection:Projection) 
                                SET 
                                projection = $props,
                                projection.id = $projectionId
                                RETURN projection
                                """, createParameters).close();
                        tx.commit();
                    }

                    return new SmartBatching<>(this.configuration, new Handler(-1)).apply(metadataEventsFlux, contextView);
                });
    }

    public Optional<ProjectionModel> getCurrentProjection(String projectionId) {
        // Check if we already have written data for this projection before
        return database.executeTransactionally("""
                MATCH (projection:Projection {id:$projectionId})
                RETURN projection.projectionPosition as projectionPosition
                """, Map.of(Projection.projectionId.name(), projectionId), result ->
                result.hasNext()
                        ? Optional.of(new ProjectionModel(result.next()))
                        : Optional.empty());
    }

    private class Handler
            implements BiConsumer<List<MetadataEvents>, SynchronousSink<List<MetadataEvents>>> {
        long position;

        public Handler(long position) {
            this.position = position;
        }

        @Override
        public void accept(List<MetadataEvents> events, SynchronousSink<List<MetadataEvents>> sink) {
            try (Transaction tx = database.beginTx()) {
                for (MetadataEvents metadataEvents : events) {
                    for (Neo4jEventProjection projection : projections) {
                        projection.write(metadataEvents, tx);
                    }
                }
                if (!events.isEmpty()) {
                    position = ((Number) events
                            .get(events.size() - 1)
                            .getMetadata()
                            .getLong(DomainEventMetadata.streamPosition.name())
                            .orElse(position + events.size()))
                            .longValue();
                    long timestamp = ((Number) events
                            .get(events.size() - 1)
                            .getMetadata()
                            .getLong(DomainEventMetadata.timestamp)
                            .orElseGet(System::currentTimeMillis))
                            .longValue();
                    Map<String, Object> updateParameters = new HashMap<>();
                    updateParameters.put(Projection.projectionId.name(), sink.contextView().get(ProjectionStreamContext.projectionId.name()));
                    updateParameters.put(Projection.projectionPosition.name(), position);
                    updateParameters.put(Projection.projectionTimestamp.name(), timestamp);
                    tx.execute("""
                            MERGE (projection:Projection {id:$projectionId}) SET 
                            projection.projectionPosition=$projectionPosition,
                            projection.projectionTimestamp=$projectionTimestamp
                            RETURN projection
                            """, updateParameters).close();
                }

                tx.commit();
            } catch (Throwable e) {
                sink.error(e);
                return;
            }
            sink.next(events);
        }
    }
}
