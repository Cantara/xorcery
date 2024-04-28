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

import com.exoreaction.xorcery.domainevents.api.DomainEvent;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4jprojections.Neo4jProjectionsConfiguration;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.RewindableEventHandler;
import com.lmax.disruptor.RewindableException;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.semconv.SemanticAttributes;
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
        implements RewindableEventHandler<MetadataEvents> {

    private final Logger logger = LogManager.getLogger(getClass());

    private final GraphDatabaseService graphDatabaseService;
    private final int eventBatchSize;
    private final List<Neo4jEventProjection> projections;

    private final Subscription subscription;
    private final Consumer<MetadataProjectionCommit> projectionCommitPublisher;

    private final String projectionId;

    private final Map<String, Object> updateParameters = new HashMap<>();

    private Transaction tx;
    private Long revision;
    private long previousRevision = 0L;
    private long lastTimestamp;
    private long currentBatchSize;
    private int eventBatchCount;

    private final LongCounter eventCounter;
    private final LongHistogram batchSize;
    private final LongHistogram commitSize;
    private final ObservableLongGauge revisionGauge;
    private final Attributes attributes;

    public Neo4jProjectionEventHandler(GraphDatabaseService graphDatabaseService,
                                       Neo4jProjectionsConfiguration configuration,
                                       Subscription subscription,
                                       Optional<ProjectionModel> projectionModel,
                                       String projectionId,
                                       Consumer<MetadataProjectionCommit> projectionCommitPublisher,
                                       List<Neo4jEventProjection> projections,
                                       OpenTelemetry openTelemetry) {
        this.eventBatchSize = configuration.eventBatchSize();
        this.projectionCommitPublisher = projectionCommitPublisher;
        this.projectionId = projectionId;

        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.projections = projections;
        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .build();
        attributes = Attributes.builder().put("projectionId", projectionId).build();
        revisionGauge = meter.gaugeBuilder("neo4j.projection.revision")
                .ofLongs().setUnit("{revision}").buildWithCallback(m -> {
            if (revision != null) m.record(revision, attributes);
        });
        eventCounter = meter.counterBuilder("neo4j.projection.events").setUnit("{event}").build();
        batchSize = meter.histogramBuilder("neo4j.projection.batchsize").setUnit("{count}").ofLongs().build();
        commitSize = meter.histogramBuilder("neo4j.projection.commitsize").setUnit("{count}").ofLongs().build();

        projectionModel.ifPresent(pm ->
        {
            pm.getProjectionPosition().ifPresent(from -> revision = from);
        });

        updateParameters.put("projection_id", projectionId);

        logger.info("Started Neo4j projection " + projectionId);
    }

    @Override
    public void onEvent(MetadataEvents event, long sequence, boolean endOfBatch) throws RewindableException, Exception {
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

                MetadataEvents cleanedMetadataEvents = event.cloneWithoutState();
                if (t instanceof EntityNotFoundException) {
                    logger.error(String.format("Could not apply Neo4j event projection update, retrying. Metadata: %s%nEvent: %s", metadataMap, cleanedMetadataEvents.getEvents()), t);
                    throw new RewindableException(t);
                } else {
                    logger.error(String.format("Could not apply Neo4j event projection update, needs to be restarted. Metadata: %s%nEvent: %s", metadataMap, cleanedMetadataEvents.getEvents()), t);

                    if (t instanceof Exception e)
                        throw e;
                    else
                        throw new Exception(t);
                }
            }
        }

        eventBatchCount += event.getEvents().size();

        if (endOfBatch || eventBatchCount >= eventBatchSize) {
            try {

                // Update Projection node with current revision
                revision = ((Number) Optional.ofNullable(metadataMap.get("revision")).orElse(0L)).longValue();
                updateParameters.put("projection_revision", revision);
                tx.execute("MERGE (projection:Projection {id:$projection_id}) SET " +
                                "projection.revision=$projection_revision,projection.projectionPosition=$projection_revision",
                        updateParameters);

                tx.commit();
                logger.trace("Updated projection " + projectionId + " to revision " + revision);

                eventCounter.add(eventBatchCount, attributes);
                commitSize.record(eventBatchCount, attributes);
                previousRevision = revision;

                // Always send commit notification, even if no changes were made
                projectionCommitPublisher.accept(new MetadataProjectionCommit(new Neo4jMetadata.Builder(new Metadata.Builder())
                        .timestamp(System.currentTimeMillis())
                        .lastTimestamp(lastTimestamp)
                        .build().context(), new ProjectionCommit(projectionId, revision)));
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
        this.batchSize.record(batchSize, attributes);
    }

    @Override
    public void onShutdown() {
        if (tx != null) {
            tx.rollback();
            tx.close();
        }

        revisionGauge.close();
    }
}
