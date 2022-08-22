package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.util.Listeners;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jDomainEventEventHandler
        implements DefaultEventHandler<Event<EventWithResult<ArrayNode, Metadata>>> {

    private Logger logger = LogManager.getLogger(getClass());

    private ReactiveEventStreams.Subscription subscription;
    private final Configuration sourceConfiguration;
    private Configuration consumerConfiguration;
    private final Listeners<ProjectionListener> listeners;
    private MetricRegistry metrics;
    private long version;
    private GraphDatabaseService graphDatabaseService;

    private Transaction tx;
    private List<CompletableFuture<Metadata>> futures = new ArrayList<>();
    private Map<String, Object> updateParameters = new HashMap<>();
    private Map<String, List<String>> cachedEventCypher = new HashMap<>();

    private final Histogram batchSize;

    public Neo4jDomainEventEventHandler(GraphDatabaseService graphDatabaseService,
                                        ReactiveEventStreams.Subscription subscription,
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
        batchSize = metrics.histogram( "neo4j.projections." + projectionId + ".batchsize" );

        sourceConfiguration.getLong("from").ifPresent(from -> version = from);

        updateParameters.put(Cypher.toField(Projection.id), projectionId);
    }

    @Override
    public synchronized void onEvent(Event<EventWithResult<ArrayNode, Metadata>> event, long sequence, boolean endOfBatch) throws Exception {

        try {

            if (tx == null)
            {
                tx = graphDatabaseService.beginTx();
            }

            ArrayNode eventsJson = event.event.event();
            Map<String, Object> metadataMap = Cypher.toMap(event.metadata.metadata());

            for (JsonNode jsonNode : eventsJson) {
                ObjectNode objectNode = (ObjectNode) jsonNode;
                String type = objectNode.path("@class").textValue();
                type = type.substring(type.lastIndexOf('$') + 1);

                Map<String, Object> parameters = Cypher.toMap(objectNode);
                parameters.put("metadata", metadataMap);

                try {
                    List<String> statement = cachedEventCypher.computeIfAbsent(type, t ->
                            event.metadata.getString("domain")
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
                            logger.error("Could not apply Neo4j statement:"+stmt, e);
                            throw e;
                        }
                    }
                } catch (Throwable e) {
                    logger.error("Could not apply Neo4j event update", e);
                    event.event.result().completeExceptionally(e);
                }
            }

            if (!event.event.result().isCompletedExceptionally())
                futures.add(event.event.result());

            if (endOfBatch) {
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
                } catch (Exception e) {
                    logger.error("Could not commit Neo4j updates", e);
                    for (CompletableFuture<Metadata> future : futures) {
                        future.completeExceptionally(e);
                    }
                } finally
                {
                    tx = null;
                }

                logger.info("Applied "+futures.size());
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
        tx.rollback();
        tx.close();
    }
}
