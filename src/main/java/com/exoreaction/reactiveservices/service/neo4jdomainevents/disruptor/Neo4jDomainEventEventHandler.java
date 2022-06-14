package com.exoreaction.reactiveservices.service.neo4jdomainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.neo4j.client.Cypher;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.internal.Files;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Neo4jDomainEventEventHandler
        implements DefaultEventHandler<Event<EventWithResult<ArrayNode, Metadata>>> {
    private ReactiveEventStreams.Subscription subscription;
    long version = 0;
    private GraphDatabaseService graphDatabaseService;

    private Transaction tx;
    private List<CompletableFuture<Metadata>> futures = new ArrayList<>();

    public Neo4jDomainEventEventHandler(GraphDatabaseService graphDatabaseService,
                                        ReactiveEventStreams.Subscription subscription) {
        this.graphDatabaseService = graphDatabaseService;
        this.subscription = subscription;
    }

    @Override
    public void onEvent(Event<EventWithResult<ArrayNode, Metadata>> event, long sequence, boolean endOfBatch) throws Exception {
        ArrayNode eventsJson = event.event.event();
        Map<String, Object> metadataMap = Cypher.toMap(event.metadata.metadata());

        for (JsonNode jsonNode : eventsJson) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            String type = objectNode.path("@class").textValue();
            type = type.substring(type.lastIndexOf('$')+1);

            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            Map<String, Object> parameters = Cypher.toMap(objectNode);
            parameters.put("metadata", metadataMap);

            try {
                String finalType = type;
                String statementFile = event.metadata.getString("domain")
                        .map(domain -> "/neo4j/" + domain + "/" + finalType + ".cyp")
                        .orElseGet(()->"/neo4j/" + finalType + ".cyp");
                String statement = Files.read(getClass().getResourceAsStream(statementFile), StandardCharsets.UTF_8);

                graphDatabaseService.executeTransactionally(statement, parameters);
            } catch (Throwable e) {
                event.event.result().completeExceptionally(e);
            }
        }

        futures.add(event.event.result());

        if (endOfBatch) {
            try {
                tx.commit();
                tx.close();
                tx = graphDatabaseService.beginTx();

                for (CompletableFuture<Metadata> future : futures) {
                    version++;
                    Metadata result = new Metadata.Builder().add("position", version).build();
                    future.complete(result);
                }
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
