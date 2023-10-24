/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.codahale.metrics.*;
import com.exoreaction.xorcery.lang.Enums;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4jprojections.Neo4jProjectionsConfiguration;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.memory.MemoryLimitExceededException;
import org.reactivestreams.Subscription;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jProjectionEventHandler
        implements EventHandler<WithMetadata<ArrayNode>> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final GraphDatabaseService graphDatabaseService;
    private int eventBatchSize;
    private List<Neo4jEventProjection> projections;
    private final Histogram batchSize;

    private final Subscription subscription;
    private final Consumer<WithMetadata<ProjectionCommit>> projectionCommitPublisher;

    private final String projectionId;

    private final Map<String, Object> updateParameters = new HashMap<>();

    private Transaction tx;
    private long version;
    private Long revision;
    private long previousRevision = 0L;
    private long lastTimestamp;
    private long currentBatchSize;
    private int eventBatchCount;
    private boolean aborted; // Set to true if projection fails
    private final Meter revisionCounter;
    private final Meter eventCounter;

    public Neo4jProjectionEventHandler(GraphDatabaseService graphDatabaseService,
                                       Neo4jProjectionsConfiguration configuration,
                                       Subscription subscription,
                                       Optional<ProjectionModel> projectionModel,
                                       String projectionId,
                                       Consumer<WithMetadata<ProjectionCommit>> projectionCommitPublisher,
                                       List<Neo4jEventProjection> projections,
                                       MetricRegistry metrics) {
        this.eventBatchSize = configuration.eventBatchSize();
        this.projectionCommitPublisher = projectionCommitPublisher;
        this.projectionId = projectionId;

        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.projections = projections;
        revisionCounter = metrics.meter("neo4j.projections." + projectionId + ".revision");
        eventCounter = metrics.meter("neo4j.projections." + projectionId + ".events");
        batchSize = metrics.histogram("neo4j.projections." + projectionId + ".batchsize");

        projectionModel.ifPresent(pm ->
        {
            pm.getVersion().ifPresent(from -> version = from);
            pm.getRevision().ifPresent(from -> revision = from);
        });

        updateParameters.put(Enums.toField(Projection.id), projectionId);

        logger.info("Started Neo4j projection " + projectionId);
    }

    @Override
    public void onEvent(WithMetadata<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {
        if (aborted)
            return;

        if (tx == null) {
            tx = graphDatabaseService.beginTx();
        }

        event.metadata().getLong("timestamp").ifPresent(value ->
        {
            lastTimestamp = Math.max(lastTimestamp, value);
        });

        ArrayNode eventsJson = event.event();
        Map<String, Object> metadataMap = Cypher.toMap(event.metadata().metadata());

        try {
            for (Neo4jEventProjection projection : projections) {
                projection.write(event, tx);
            }
        } catch (Throwable t) {
            logger.error(String.format("Could not apply Neo4j event projection update, needs to be restarted. Metadata: %s%nEvent: %s", metadataMap, eventsJson.toPrettyString()), t);
            tx.rollback();
            tx.close();
            tx = null;
            aborted = true;

            if (t instanceof Exception e)
                throw e;
            else
                throw new Exception(t);
        }

        version++;
        eventBatchCount += event.event().size();

        if (endOfBatch || eventBatchCount >= eventBatchSize) {
            try {
                // Update Projection node with current revision
                updateParameters.put("projection_version", version);
                revision = ((Number) Optional.ofNullable(metadataMap.get("revision")).orElse(0L)).longValue();
                updateParameters.put("projection_revision", revision);
                tx.execute("MERGE (projection:Projection {id:$projection_id}) SET " +
                                "projection.version=$projection_version, " +
                                "projection.revision=$projection_revision",
                        updateParameters);

                tx.commit();
                tx.close();
                logger.trace("Updated projection " + projectionId + " to revision " + revision);

                revisionCounter.mark(revision - previousRevision);
                eventCounter.mark(eventBatchCount);
                previousRevision = revision;
            } catch (Throwable t) {
                logger.error("Could not commit Neo4j updates", t);
                tx.close();

                if (t instanceof Exception e)
                    throw e;
                else
                    throw new Exception(t);
            }

            logger.trace("Applied {} commands, {} events", currentBatchSize, eventBatchCount);

            eventBatchCount = 0;

            // Always send commit notification, even if no changes were made
            projectionCommitPublisher.accept(new WithMetadata<>(new Neo4jMetadata.Builder(new Metadata.Builder())
                    .timestamp(System.currentTimeMillis())
                    .lastTimestamp(lastTimestamp)
                    .build().context(), new ProjectionCommit(projectionId, version)));

            tx = null;

            if (endOfBatch)
                subscription.request(currentBatchSize);
        }
    }


    @Override
    public void onBatchStart(long batchSize, long queueDepth) {
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
