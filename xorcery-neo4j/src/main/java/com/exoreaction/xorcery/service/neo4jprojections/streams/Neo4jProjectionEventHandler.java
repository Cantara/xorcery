package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.util.Enums;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jProjectionEventHandler
        implements EventHandler<WithMetadata<ArrayNode>> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final GraphDatabaseService graphDatabaseService;
    private List<Neo4jEventProjection> projections;
    private final Histogram batchSize;

    private final Flow.Subscription subscription;
    private final Consumer<WithMetadata<ProjectionCommit>> projectionCommitPublisher;

    private final String projectionId;

    private final Map<String, Object> updateParameters = new HashMap<>();
    private final Map<String, Neo4jEventProjection> eventProjections = new HashMap<>();

    private Transaction tx;
    private long version;
    private long lastTimestamp;
    private int appliedEvents;
    private long currentBatchSize;

    public Neo4jProjectionEventHandler(GraphDatabaseService graphDatabaseService,
                                       Flow.Subscription subscription,
                                       Optional<Long> fromVersion,
                                       String projectionId,
                                       Consumer<WithMetadata<ProjectionCommit>> projectionCommitPublisher,
                                       List<Neo4jEventProjection> projections,
                                       MetricRegistry metrics) {
        this.projectionCommitPublisher = projectionCommitPublisher;
        this.projectionId = projectionId;

        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.projections = projections;
        metrics.gauge("neo4j.projections." + projectionId + ".revision", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> () -> version);
        batchSize = metrics.histogram("neo4j.projections." + projectionId + ".batchsize");

        fromVersion.ifPresent(from -> version = from);

        updateParameters.put(Enums.toField(Projection.id), projectionId);
    }

    @Override
    public void onEvent(WithMetadata<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {
        try {

            if (tx == null) {
                tx = graphDatabaseService.beginTx();
            }

            event.metadata().getLong("timestamp").ifPresent(value ->
            {
                lastTimestamp = Math.max(lastTimestamp, value);
            });

            ArrayNode eventsJson = event.event();
            Map<String, Object> metadataMap = Cypher.toMap(event.metadata().metadata());

            for (JsonNode jsonNode : eventsJson) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                String type = objectNode.path("@class").textValue();

                try {

                    Neo4jEventProjection projection = eventProjections.computeIfAbsent(type, t ->
                    {
                        for (Neo4jEventProjection eventProjection : projections) {
                            if (eventProjection.isWritable(t))
                            {
                                return eventProjection;
                            }
                        }
                        throw new IllegalStateException("No projection can handle event with type:"+t);
                    });

                    projection.write(metadataMap, objectNode, tx);
                } catch (Throwable e) {
                    logger.error("Could not apply Neo4j event update", e);

                    tx = graphDatabaseService.beginTx();
                }

                appliedEvents++;
                version++;
            }

            if (endOfBatch) {
                if (appliedEvents > 0) {
                    try {
                        // Update Projection node with current revision
                        updateParameters.put("projection_revision", version);
                        tx.execute("MERGE (projection:Projection {id:$projection_id}) SET projection.revision=$projection_revision",
                                updateParameters);

                        tx.commit();
                        tx.close();
                    } catch (Throwable e) {
                        logger.error("Could not commit Neo4j updates", e);
                    }

                    logger.info("Applied " + currentBatchSize);

                    appliedEvents = 0;
                } else {
                    tx.commit();
                    tx.close();
                }

                // Always send commit notification, even if no changes were made
                projectionCommitPublisher.accept(new WithMetadata<>(new Neo4jMetadata.Builder(new Metadata.Builder())
                        .timestamp(System.currentTimeMillis())
                        .lastTimestamp(lastTimestamp)
                        .build().context(), new ProjectionCommit(projectionId, version)));

                tx = null;

                subscription.request(currentBatchSize);
            }

        } catch (Exception e) {
            logger.error("Could not update Neo4j event projection");
        }
    }

    @Override
    public void onBatchStart(long batchSize) {
        this.currentBatchSize = batchSize;
        this.batchSize.update(batchSize);
    }

    @Override
    public void onShutdown() {
        if (tx != null) {
            tx.rollback();
            tx.close();
        }
    }
}
