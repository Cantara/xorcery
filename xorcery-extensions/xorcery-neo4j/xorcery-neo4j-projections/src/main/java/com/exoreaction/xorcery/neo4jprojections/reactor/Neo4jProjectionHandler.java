package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.api.reactor.ContextViewElement;
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

import static com.exoreaction.xorcery.reactivestreams.api.reactor.ContextViewElement.missing;

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

        try {
            String projectionId = new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId)
                    .orElseThrow(missing(ProjectionStreamContext.projectionId));
            logger.info("Starting Neo4j projection with id " + projectionId);
            Optional<ProjectionModel> currentProjection = getCurrentProjection(projectionId);
            return currentProjection.flatMap(ProjectionModel::getProjectionPosition)
                    .map(p -> new SmartBatching<>(this.configuration, new Handler(p)).apply(metadataEventsFlux.contextWrite(Context.of(ProjectionStreamContext.projectionPosition, p)), contextView))
                    .orElseGet(() ->
                    {
                        // Create projection in Neo4j
                        try (Transaction tx = database.beginTx()) {
                            Map<String, Object> createParameters = new HashMap<>();
                            createParameters.put(Projection.projectionId.name(), projectionId);
                            Map<String, String> props = new HashMap<>();
                            contextView.forEach((k, v) ->
                            {
                                if (!k.toString().equals(ProjectionStreamContext.projectionId.name())) {
                                    props.put(k.toString(), v.toString());
                                }
                            });
                            createParameters.put("props", props);
                            tx.execute("""
                                    MERGE (projection:Projection {id:$projectionId})
                                    ON CREATE SET
                                    projection.projectionPosition = -1,
                                    projection += $props
                                    ON MATCH SET
                                    projection.projectionPosition = coalesce(projection.revision,-1),
                                    projection += $props
                                    RETURN projection
                                    """, createParameters).close();
                            tx.commit();
                        }

                        return new SmartBatching<>(this.configuration, new Handler(-1)).apply(metadataEventsFlux, contextView);
                    });
        } catch (RuntimeException e) {
            return Flux.error(e);
        }
    }

    public Optional<ProjectionModel> getCurrentProjection(String projectionId) {
        // Check if we already have written data for this projection before
        return database.executeTransactionally("""
                MATCH (projection:Projection {id:$projectionId})
                RETURN coalesce(projection.projectionPosition, projection.revision) as projectionPosition
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
                String projectionId = new ContextViewElement(sink.contextView()).getString(ProjectionStreamContext.projectionId)
                        .orElseThrow(missing(ProjectionStreamContext.projectionId));
                for (MetadataEvents metadataEvents : events) {
                    metadataEvents.getMetadata().toBuilder().add(ProjectionStreamContext.projectionId, projectionId);
                    for (Neo4jEventProjection projection : projections) {
                        projection.write(metadataEvents, tx);
                    }
                }
                if (!events.isEmpty()) {
                    position = ((Number) events
                            .get(events.size() - 1)
                            .getMetadata()
                            .getLong(DomainEventMetadata.streamPosition)
                            .orElse(position + events.size()))
                            .longValue();
                    long timestamp = ((Number) events
                            .get(events.size() - 1)
                            .getMetadata()
                            .getLong(DomainEventMetadata.timestamp)
                            .orElseGet(System::currentTimeMillis))
                            .longValue();
                    Map<String, Object> updateParameters = new HashMap<>();
                    updateParameters.put(Projection.projectionId.name(), projectionId);
                    updateParameters.put(Projection.projectionPosition.name(), position);
                    updateParameters.put(Projection.projectionTimestamp.name(), timestamp);
                    tx.execute("""
                            MATCH (projection:Projection {id:$projectionId}) SET 
                            projection.projectionPosition=$projectionPosition,
                            projection.projectionTimestamp=$projectionTimestamp
                            RETURN projection
                            """, updateParameters).close();

                    System.out.println("Committed "+events.size());
//                    Thread.sleep(1000);
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
