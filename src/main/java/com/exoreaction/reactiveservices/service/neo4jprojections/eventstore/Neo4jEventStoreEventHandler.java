package com.exoreaction.reactiveservices.service.neo4jprojections.eventstore;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.api.EventStoreMetadata;
import com.exoreaction.reactiveservices.service.eventstore.resources.api.EventStoreParameters;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.exoreaction.reactiveservices.service.neo4jprojections.ProjectionListener;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.util.Listeners;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.internal.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jEventStoreEventHandler
        implements DefaultEventHandler<Event<ArrayNode>> {

    private Logger logger = LogManager.getLogger(getClass());

    private ReactiveEventStreams.Subscription subscription;
    private EventStoreParameters sourceParameters;
    private Listeners<ProjectionListener> listeners;
    private GraphDatabaseService graphDatabaseService;

    private Transaction tx;
    private Map<String, Object> updateParameters = new HashMap<>();

    public Neo4jEventStoreEventHandler(GraphDatabaseService graphDatabaseService,
                                       ReactiveEventStreams.Subscription subscription,
                                       EventStoreParameters sourceParameters,
                                       Listeners<ProjectionListener> listeners) {
        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
        this.sourceParameters = sourceParameters;
        this.listeners = listeners;

        updateParameters.put("stream_name", sourceParameters.stream);
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
                String finalType = type;
                String statementFile = event.metadata.getString("domain")
                        .map(domain -> "/neo4j/" + domain + "/" + finalType + ".cyp")
                        .orElseGet(() -> "/neo4j/" + finalType + ".cyp");
                String statement = Files.read(getClass().getResourceAsStream(statementFile), StandardCharsets.UTF_8);

                tx.execute(statement, parameters);
            } catch (Throwable e) {
                logger.error("Could not apply event to Neo4j projection", e);
            }
        }

        if (endOfBatch) {
            try {
                // Update Stream node with current revision
                long revision = new EventStoreMetadata(event.metadata).revision();
                updateParameters.put("stream_revision", revision);
                tx.execute("MERGE (stream:Stream {name:$stream_name}) SET stream.revision=$stream_revision",
                        updateParameters);

                tx.commit();
                tx.close();
                tx = graphDatabaseService.beginTx();

                listeners.listener().onCommit(sourceParameters.stream, revision);
            } catch (Exception e) {
                logger.error("Could not commit Neo4j projection updates", e);
            }
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
