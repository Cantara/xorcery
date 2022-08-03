package com.exoreaction.xorcery.service.neo4jprojections.eventstore;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.util.Listeners;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.xorcery.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jEventStoreEventHandler
        implements DefaultEventHandler<Event<ArrayNode>> {

    private final String projectionId;
    private Logger logger = LogManager.getLogger(getClass());

    private ReactiveEventStreams.Subscription subscription;
    private EventStoreParameters sourceParameters;
    private Configuration consumerConfiuration;
    private Listeners<ProjectionListener> listeners;
    private GraphDatabaseService graphDatabaseService;

    private long lastRevision;
    private long lastBatchSize = Long.MAX_VALUE;
    private long batchSize = 0;
    private CompletableFuture<Void> isLive;

    private Transaction tx;
    private Map<String, Object> updateParameters = new HashMap<>();
    private Map<String, String> cachedEventCypher = new HashMap<>();

    public Neo4jEventStoreEventHandler(GraphDatabaseService graphDatabaseService,
                                       ReactiveEventStreams.Subscription subscription,
                                       EventStoreParameters sourceParameters,
                                       Configuration consumerConfiuration,
                                       Listeners<ProjectionListener> listeners,
                                       long lastRevision,
                                       CompletableFuture<Void> isLive) {
        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.sourceParameters = sourceParameters;
        this.consumerConfiuration = consumerConfiuration;
        this.listeners = listeners;
        this.lastRevision = lastRevision;
        this.isLive = isLive;

        projectionId = consumerConfiuration.getString("id").orElse("default");
        updateParameters.put(Cypher.toField(Projection.id), projectionId);
    }

    @Override
    public void onEvent(Event<ArrayNode> event, long sequence, boolean endOfBatch) throws Exception {
        ArrayNode eventsJson = event.event;
        Map<String, Object> metadataMap = Cypher.toMap(event.metadata.metadata());

        for (JsonNode jsonNode : eventsJson) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            String type = objectNode.path("@class").textValue();
            type = type.substring(type.lastIndexOf('$') + 1);

            Map<String, Object> parameters = Cypher.toMap(objectNode);
            parameters.put("metadata", metadataMap);

            try {
                String statement = cachedEventCypher.computeIfAbsent(type, t ->
                        event.metadata.getString("domain")
                                .map(domain ->
                                {
                                    String statementFile = "/neo4j/" + domain + "/" + t + ".cyp";
                                    try (InputStream resourceAsStream = getClass().getResourceAsStream(statementFile)) {
                                        if (resourceAsStream == null)
                                            return null;

                                        return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
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

                                        return new String(resourceAsStream.readAllBytes(), StandardCharsets.UTF_8);
                                    } catch (IOException e) {
                                        logger.error("Could not load Neo4j event projection Cypher statement:" + statementFile, e);
                                        return null;
                                    }
                                }));
                if (statement == null)
                    break;

                tx.execute(statement, parameters);
            } catch (Throwable e) {
                logger.error("Could not apply event to Neo4j projection", e);
            }
        }

        if (endOfBatch) {
            try {
                // Update Projection node with current revision
                long revision = new EventStoreMetadata(event.metadata).revision();
                updateParameters.put("projection_revision", revision);
                tx.execute("MERGE (projection:Projection {id:$projection_id}) SET projection.revision=$projection_revision",
                        updateParameters);

                tx.commit();
                tx.close();
                tx = graphDatabaseService.beginTx();

                listeners.listener().onCommit(sourceParameters.stream, revision);

                if (!isLive.isDone())
                {
                    // TODO This should have added test since stream might have progressed significantly since start of catch-up
                    if (revision >= lastRevision )
                    {
                        logger.info("Projection "+projectionId+" is now live");
                        isLive.complete(null);
                    }
                    lastBatchSize = batchSize;
                }

            } catch (Exception e) {
                logger.error("Could not commit Neo4j projection updates", e);
            }
        }

        subscription.request(1);
    }

    @Override
    public void onBatchStart(long batchSize) {
        this.batchSize = batchSize;
    }

    @Override
    public void onStart() {
        tx = graphDatabaseService.beginTx();
    }

    @Override
    public void onShutdown() {
        tx.commit();
        tx.close();
    }
}
