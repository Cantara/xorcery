package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.neo4jprojections.api.ProjectionCommit;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private final MetricRegistry metrics;
    private final Histogram batchSize;

    private final Flow.Subscription subscription;
    private final Consumer<WithMetadata<ProjectionCommit>> projectionCommitPublisher;
    private final Configuration consumerConfiguration;

    private final String projectionId;

    private final Map<String, Object> updateParameters = new HashMap<>();
    private final Map<String, List<String>> cachedEventCypher = new HashMap<>();

    private Transaction tx;
    private long version;
    private long lastTimestamp;
    private int appliedEvents;
    private long currentBatchSize;

    public Neo4jProjectionEventHandler(GraphDatabaseService graphDatabaseService,
                                       Flow.Subscription subscription,
                                       Optional<Long> fromVersion,
                                       Configuration consumerConfiguration,
                                       Consumer<WithMetadata<ProjectionCommit>> projectionCommitPublisher,
                                       MetricRegistry metrics) {
        this.projectionCommitPublisher = projectionCommitPublisher;
        projectionId = consumerConfiguration.getString(Projection.id.name()).orElseThrow();

        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.consumerConfiguration = consumerConfiguration;
        this.metrics = metrics;
        metrics.gauge("neo4j.projections." + projectionId + ".revision", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> () -> version);
        batchSize = metrics.histogram("neo4j.projections." + projectionId + ".batchsize");

        fromVersion.ifPresent(from -> version = from);

        updateParameters.put(Cypher.toField(Projection.id), projectionId);
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

                Map<String, Object> parameters = Cypher.toMap(objectNode);
                parameters.put("metadata", metadataMap);

                try {
                    List<String> statement = cachedEventCypher.computeIfAbsent(type, t ->
                    {
                        if (t.indexOf('$') == -1) {
                            // Normal type, strip package
                            t = t.substring(t.lastIndexOf('.') + 1);
                        } else {
                            // For enclosed types
                            t = t.substring(t.lastIndexOf('$') + 1);
                        }

                        final String shortenedType = t;

                        return event.metadata().getString("domain")
                                .map(domain ->
                                {
                                    String statementFile = "/neo4j/" + domain + "/" + shortenedType + ".cyp";
                                    try (InputStream resourceAsStream = getClass().getResourceAsStream(statementFile)) {
                                        if (resourceAsStream == null)
                                            return null;

                                        return List.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).split(";"));
                                    } catch (IOException e) {
                                        logger.error("Could not load Neo4j event projection Cypher statement:" + statementFile, e);
                                        return null;
                                    }
                                })
                                .orElseGet(() ->
                                        {
                                            String statementFile = "/neo4j/" + shortenedType + ".cyp";
                                            try (InputStream resourceAsStream = getClass().getResourceAsStream(statementFile)) {
                                                if (resourceAsStream == null)
                                                    return null;

                                                return List.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).split(";"));
                                            } catch (IOException e) {
                                                logger.error("Could not load Neo4j event projection Cypher statement:" + statementFile, e);
                                                return null;
                                            }
                                        }
                                );
                    });
                    if (statement == null)
                        break;

                    for (String stmt : statement) {
                        try {
                            tx.execute(stmt, parameters);
                        } catch (Throwable e) {
                            logger.error(String.format("Could not apply Neo4j statement for event %s (metadata:%s,parameters:%s):\n%s", type, metadataMap.toString(), parameters.toString(), stmt), e);
                            throw e;
                        }
                    }
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
