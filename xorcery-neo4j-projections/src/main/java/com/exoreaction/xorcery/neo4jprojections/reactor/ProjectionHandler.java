package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.lang.Enums;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.reactor.spi.EventsWithTransaction;
import com.exoreaction.xorcery.neo4jprojections.reactor.spi.Neo4jProjection;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.BiConsumer;

public class ProjectionHandler
        implements BiConsumer<List<MetadataEvents>, SynchronousSink<List<MetadataEvents>>>, SynchronousSink<List<MetadataEvents>> {
    @Service
    public static class Factory<T> {
        private final GraphDatabase database1;
        private final IterableProvider<Neo4jProjection> projections1;

        @Inject
        public Factory(GraphDatabase database, IterableProvider<Neo4jProjection> projections) {
            database1 = database;
            projections1 = projections;
        }

        public ProjectionHandler create(String projectionId, Optional<ProjectionModel> currentProjection) {
            return new ProjectionHandler(database1, projections1, projectionId, currentProjection);
        }
    }

    private final GraphDatabaseService database;
    private final Optional<ProjectionModel> currentProjection;
    private final List<Neo4jProjection> projections = new ArrayList<>();
    private final Map<String, Object> updateParameters = new HashMap<>();

    private List<MetadataEvents> next;
    private Throwable e;
    private boolean complete;
    private SynchronousSink<List<MetadataEvents>> sink;

    private long version;

    @Inject
    public ProjectionHandler(GraphDatabase database, IterableProvider<Neo4jProjection> projections, String projectionId, Optional<ProjectionModel> currentProjection) {
        this.database = database.getGraphDatabaseService();
        this.currentProjection = currentProjection;
        updateParameters.put(Enums.toField(Projection.id), projectionId);
        projections.forEach(this.projections::add);
        currentProjection.ifPresent(p -> version = p.getVersion().orElse(0L));
    }

    @Override
    public void complete() {
        complete = true;
    }

    @Override
    public Context currentContext() {
        return sink.currentContext();
    }

    @Override
    public void error(Throwable e) {
        this.e = e;
    }

    @Override
    public void next(List<MetadataEvents> t) {
        next = t;
    }

    @Override
    public ContextView contextView() {
        return sink.contextView();
    }

    @Override
    public void accept(List<MetadataEvents> item, SynchronousSink<List<MetadataEvents>> sink) {
        this.sink = sink;
        EventsWithTransaction itemEventsWithTransaction = null;
        try (Transaction tx = database.beginTx()) {
            itemEventsWithTransaction = new EventsWithTransaction(tx, item);
            for (Neo4jProjection projection : projections) {
                projection.accept(itemEventsWithTransaction, this);
                if (complete) {
                    sink.complete();
                } else if (e != null) {
                    sink.error(e);
                    return;
                }
                itemEventsWithTransaction = new EventsWithTransaction(tx, next);
            }
            // TODO Write projection state to database
            if (!item.isEmpty()) {
                updateParameters.put("projection_version", version);
                long revision = ((Number) item.get(item.size() - 1).getMetadata().getLong("revision").orElse(0L)).longValue();
                long timestamp = ((Number) item.get(item.size() - 1).getMetadata().getLong("timestamp").orElseGet(System::currentTimeMillis)).longValue();
                updateParameters.put("projection_revision", revision);
                updateParameters.put("projection_timestamp", timestamp);
                tx.execute("MERGE (projection:Projection {id:$projection_id}) SET " +
                                "projection.timestamp=$projection_timestamp, " +
                                "projection.version=$projection_version, " +
                                "projection.revision=$projection_revision",
                        updateParameters).close();
            }

            tx.commit();
        } catch (Throwable e) {
            sink.error(e);
        }
        if (itemEventsWithTransaction != null)
            sink.next(itemEventsWithTransaction.event());
    }

}
