package com.exoreaction.xorcery.service.domaineventsapiaggregate;

import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.domainevents.api.aggregate.Aggregate;
import com.exoreaction.xorcery.service.domainevents.api.aggregate.AggregateSnapshot;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.RowModel;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotFoundException;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Neo4jAggregateSnapshotLoader {

    private final ObjectMapper objectMapper;
    private GraphDatabase database;
    private Map<Class<?>, String> cypherCache = new ConcurrentHashMap<>();


    public Neo4jAggregateSnapshotLoader(GraphDatabase database) {

        this.database = database;
        this.objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }

    public <T extends AggregateSnapshot> T load(DomainEventMetadata metadata, Aggregate<T> aggregate)
            throws IOException {
        String cypher = cypherCache.computeIfAbsent(aggregate.getClass(),
                new Function<Class<?>, String>() {
                    @Override
                    public String apply(Class clazz) {
                        String statementFile = "/neo4j/" +metadata.getDomain()+"/snapshot/" + clazz.getSimpleName() + ".cyp";
                        try {
                            return new String(Objects.requireNonNull(getClass().getResourceAsStream(statementFile)).readAllBytes(), StandardCharsets.UTF_8);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                });

        Map<String, Object> metadataMap = Cypher.toMap(metadata.metadata().metadata());

        return database.execute(cypher, Map.of("metadata", metadataMap), 30)
                .thenCompose(result ->
                {
                    try (result) {
                        AtomicReference<T> snapshot = new AtomicReference<>();
                        result.getResult().accept(new Result.ResultVisitor<Exception>() {
                            @Override
                            public boolean visit(Result.ResultRow resultRow) throws Exception {
                                RowModel rowModel = new RowModel(resultRow);
                                ObjectNode json = JsonNodeFactory.instance.objectNode();
                                for (String columnName : result.getResult().columns()) {
                                    json.set(columnName, rowModel.getJsonNode(columnName));
                                }
                                snapshot.set((T) objectMapper.treeToValue(json, aggregate.getSnapshot().getClass()));
                                return false;
                            }
                        });

                        if (snapshot.get() == null)
                        {
                            return CompletableFuture.failedStage(new NotFoundException(metadata.getAggregateId()));
                        }

                        return CompletableFuture.completedStage(snapshot.get());
                    } catch (Throwable t) {
                        return CompletableFuture.failedStage(t);
                    }
                }).toCompletableFuture().join();
    }
}
