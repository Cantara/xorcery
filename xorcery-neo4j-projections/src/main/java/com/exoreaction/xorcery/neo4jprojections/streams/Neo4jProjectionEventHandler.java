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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.domainevents.api.CommandEvents;
import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.lang.Enums;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4jprojections.Neo4jProjectionsConfiguration;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.RewindableEventHandler;
import com.lmax.disruptor.RewindableException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.exceptions.EntityNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
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
        implements RewindableEventHandler<CommandEvents> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final GraphDatabaseService graphDatabaseService;
    private final int eventBatchSize;
    private final List<Neo4jEventProjection> projections;

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

    private final Meter revisionCounter;
    private final Meter eventCounter;
    private final Histogram batchSize;
    private final Histogram commitSize;

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
        commitSize = metrics.histogram("neo4j.projections." + projectionId + ".commitsize");

        projectionModel.ifPresent(pm ->
        {
            pm.getVersion().ifPresent(from -> version = from);
            pm.getRevision().ifPresent(from -> revision = from);
        });

        updateParameters.put(Enums.toField(Projection.id), projectionId);

        logger.info("Started Neo4j projection " + projectionId);
    }

    @Override
    public void onEvent(CommandEvents event, long sequence, boolean endOfBatch) throws RewindableException, Exception {
        if (tx == null) {
            tx = graphDatabaseService.beginTx();
        }

        event.getMetadata().getLong("timestamp").ifPresent(value ->
        {
            lastTimestamp = Math.max(lastTimestamp, value);
        });

        Map<String, Object> metadataMap = Cypher.toMap(event.getMetadata().metadata());
        // Preprocessor may have removed all the events, so check for this
        List<DomainEvent> domainEvents = event.getEvents();
        if (!domainEvents.isEmpty()) {

            try {
                for (Neo4jEventProjection projection : projections) {
                    projection.write(event, tx);
                }
            } catch (Throwable t) {
                tx.rollback();
                tx.close();
                tx = null;

                if (t instanceof EntityNotFoundException) {
                    logger.error(String.format("Could not apply Neo4j event projection update, retrying. Metadata: %s%nEvent: %s", metadataMap, domainEvents), t);
                    throw new RewindableException(t);
                } else {
                    logger.error(String.format("Could not apply Neo4j event projection update, needs to be restarted. Metadata: %s%nEvent: %s", metadataMap, domainEvents), t);

                    if (t instanceof Exception e)
                        throw e;
                    else
                        throw new Exception(t);
                }
            }
        }

        version++;
        eventBatchCount += event.getEvents().size();

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
                logger.trace("Updated projection " + projectionId + " to revision " + revision);

                revisionCounter.mark(revision - previousRevision);
                eventCounter.mark(eventBatchCount);
                commitSize.update(eventBatchCount);
                previousRevision = revision;

                // Always send commit notification, even if no changes were made
                projectionCommitPublisher.accept(new WithMetadata<>(new Neo4jMetadata.Builder(new Metadata.Builder())
                        .timestamp(System.currentTimeMillis())
                        .lastTimestamp(lastTimestamp)
                        .build().context(), new ProjectionCommit(projectionId, version)));
            } catch (Throwable t) {
                logger.error("Could not commit Neo4j updates", t);

                if (t instanceof Exception e)
                    throw e;
                else
                    throw new Exception(t);
            } finally {
                tx.close();
            }

            logger.trace("Applied {} commands, {} events", currentBatchSize, eventBatchCount);

            eventBatchCount = 0;

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
