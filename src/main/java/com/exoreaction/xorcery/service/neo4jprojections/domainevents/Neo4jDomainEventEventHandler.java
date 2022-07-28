package com.exoreaction.xorcery.service.neo4jprojections.domainevents;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.xorcery.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4jprojections.ProjectionListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.util.Listeners;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.internal.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    private final Listeners<ProjectionListener> listeners;
    long version = 0;
    private GraphDatabaseService graphDatabaseService;

    private Transaction tx;
    private List<CompletableFuture<Metadata>> futures = new ArrayList<>();
    private Map<String, Object> updateParameters = new HashMap<>();
    private Map<String, List<String>> cachedEventCypher = new HashMap<>();

    public Neo4jDomainEventEventHandler(GraphDatabaseService graphDatabaseService,
                                        ReactiveEventStreams.Subscription subscription,
                                        Configuration sourceConfiguration,
                                        Listeners<ProjectionListener> listeners) {
        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.sourceConfiguration = sourceConfiguration;
        this.listeners = listeners;

        sourceConfiguration.getLong("from").ifPresent(from -> version = from);

        updateParameters.put("projection_name", sourceConfiguration.getString("stream").orElseThrow());
    }

    @Override
    public void onEvent(Event<EventWithResult<ArrayNode, Metadata>> event, long sequence, boolean endOfBatch) throws Exception {
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
                    tx.execute(stmt, parameters);
                }
            } catch (Throwable e) {
                event.event.result().completeExceptionally(e);
            }
        }

        if (!event.event.result().isCompletedExceptionally())
            futures.add(event.event.result());

        if (endOfBatch) {
            try {
                // Update Projection node with current revision
                updateParameters.put("projection_revision", version + futures.size());
                tx.execute("MERGE (projection:Projection {name:$projection_name}) SET projection.revision=$projection_revision",
                        updateParameters);

                tx.commit();
                tx.close();
                tx = graphDatabaseService.beginTx();

                for (CompletableFuture<Metadata> future : futures) {
                    version++;
                    Metadata result = new Metadata.Builder().add("revision", version).build();
                    future.complete(result);
                }
                System.out.println("Completed future " + version);
                listeners.listener().onCommit(sourceConfiguration.getString("stream").orElse(""), version);

            } catch (Exception e) {
                for (CompletableFuture<Metadata> future : futures) {
                    future.completeExceptionally(e);
                }
            }
            futures.clear();
        }

        subscription.request(1);
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
