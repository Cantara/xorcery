package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.exoreaction.xorcery.util.Listeners;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jDomainEventEventHandler
        implements EventHandler<WithResult<WithMetadata<ArrayNode>, Metadata>> {

    private Logger logger = LogManager.getLogger(getClass());

    private Flow.Subscription subscription;
    private final Configuration sourceConfiguration;
    private Configuration consumerConfiguration;
    private final Listeners<ProjectionListener> listeners;
    private MetricRegistry metrics;
    private long version;
    private int appliedEvents;
    private GraphDatabaseService graphDatabaseService;

    private Transaction tx;
    private List<CompletableFuture<Metadata>> futures = new ArrayList<>();
    private Map<String, Object> updateParameters = new HashMap<>();
    private Map<String, List<String>> cachedEventCypher = new HashMap<>();

    private final Histogram batchSize;

    public Neo4jDomainEventEventHandler(GraphDatabaseService graphDatabaseService,
                                        Flow.Subscription subscription,
                                        Configuration sourceConfiguration,
                                        Configuration consumerConfiguration,
                                        Listeners<ProjectionListener> listeners,
                                        MetricRegistry metrics) {
        String projectionId = consumerConfiguration.getString(Projection.id.name()).orElseThrow();

        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.sourceConfiguration = sourceConfiguration;
        this.consumerConfiguration = consumerConfiguration;
        this.listeners = listeners;
        this.metrics = metrics;
        metrics.gauge("neo4j.projections." + projectionId + ".revision", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> () -> version);
        batchSize = metrics.histogram("neo4j.projections." + projectionId + ".batchsize");

        sourceConfiguration.getLong("from").ifPresent(from -> version = from);

        updateParameters.put(Cypher.toField(Projection.id), projectionId);
    }

    @Override
    public void onEvent(WithResult<WithMetadata<ArrayNode>, Metadata> event, long sequence, boolean endOfBatch) throws Exception {
        try {

            if (tx == null) {
                tx = graphDatabaseService.beginTx();
            }

            ArrayNode eventsJson = event.event().event();
            Map<String, Object> metadataMap = Cypher.toMap(event.event().metadata().metadata());

            for (JsonNode jsonNode : eventsJson) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                String type = objectNode.path("@class").textValue();
                type = type.substring(type.lastIndexOf('$') + 1);

                Map<String, Object> parameters = Cypher.toMap(objectNode);
                parameters.put("metadata", metadataMap);

                try {
                    List<String> statement = cachedEventCypher.computeIfAbsent(type, t ->
                            event.event().metadata().getString("domain")
                                    .map(domain ->
                                    {
                                        String statementFile = "/neo4j/" + domain + "/" + t + ".cyp";
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
                                        String statementFile = "/neo4j/" + t + ".cyp";
                                        try (InputStream resourceAsStream = getClass().getResourceAsStream(statementFile)) {
                                            if (resourceAsStream == null)
                                                return null;

                                            return List.of(new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8).split(";"));
                                        } catch (IOException e) {
                                            logger.error("Could not load Neo4j event projection Cypher statement:" + statementFile, e);
                                            return null;
                                        }
                                    }));
                    if (statement == null)
                        break;

                    for (String stmt : statement) {
                        try {
                            tx.execute(stmt, parameters);
                        } catch (Throwable e) {
                            logger.error(String.format("Could not apply Neo4j statement for event %s (metadata:%s,parameters:%s):\n%s",type,metadataMap.toString(),parameters.toString(),stmt), e);
                            throw e;
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Could not apply Neo4j event update", e);
                    event.result().completeExceptionally(e);
                    tx = graphDatabaseService.beginTx();
                }

                appliedEvents++;
            }

            if (!event.result().isCompletedExceptionally())
                futures.add(event.result());

            if (endOfBatch) {
                if (appliedEvents > 0) {
                    try {
                        // Update Projection node with current revision
                        updateParameters.put("projection_revision", version + futures.size());
                        tx.execute("MERGE (projection:Projection {id:$projection_id}) SET projection.revision=$projection_revision",
                                updateParameters);

                        tx.commit();
                        tx.close();

                        for (CompletableFuture<Metadata> future : futures) {
                            version++;
                            Metadata result = new Metadata.Builder().add("revision", version).build();
                            future.complete(result);
                        }
                        listeners.listener().onCommit(sourceConfiguration.getString("stream").orElse(""), version);

                        batchSize.update(futures.size());
                    } catch (Throwable e) {
                        logger.error("Could not commit Neo4j updates", e);
                        for (CompletableFuture<Metadata> future : futures) {
                            future.completeExceptionally(e);
                        }
                    }

                    logger.info("Applied " + futures.size());
                } else {
                    // No changes applied
                    for (CompletableFuture<Metadata> future : futures) {
                        Metadata result = new Metadata.Builder().add("revision", version).build();
                        future.complete(result);
                    }
                }
                appliedEvents = 0;
                tx = null;

                subscription.request(futures.size());
                futures.clear();
            }

        } catch (Exception e) {
            logger.error("Could not update Neo4j event projection");
        }

//        subscription.request(1);
    }

    @Override
    public void onShutdown() {
        if (tx != null)
        {
            tx.rollback();
            tx.close();
        }
    }
}
