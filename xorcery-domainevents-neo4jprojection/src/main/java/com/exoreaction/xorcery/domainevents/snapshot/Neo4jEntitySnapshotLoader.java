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
package com.exoreaction.xorcery.domainevents.snapshot;

import com.exoreaction.xorcery.domainevents.helpers.context.DomainEventMetadata;
import com.exoreaction.xorcery.domainevents.helpers.entity.Entity;
import com.exoreaction.xorcery.domainevents.helpers.entity.EntitySnapshot;
import com.exoreaction.xorcery.domainevents.helpers.model.CommonModel;
import com.exoreaction.xorcery.neo4j.client.Cypher;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4j.client.RowModel;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.NotFoundException;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class Neo4jEntitySnapshotLoader {

    private final ObjectMapper objectMapper;
    private GraphDatabase database;
    private Map<Class<?>, String> cypherCache = new ConcurrentHashMap<>();


    public Neo4jEntitySnapshotLoader(GraphDatabase database) {

        this.database = database;
        this.objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }

    public <T extends EntitySnapshot> T load(DomainEventMetadata metadata, String entityId, Entity<T> entity)
            throws IOException {

        String cypher = cypherCache.computeIfAbsent(entity.getClass(),
                new Function<Class<?>, String>() {
                    @Override
                    public String apply(Class clazz) {
                        String statementFile = "META-INF/neo4j/snapshot/" + clazz.getSimpleName() + ".cyp";
                        return Resources.getResource(statementFile).map(url ->
                        {
                            try (InputStream inputStream = url.openStream()) {
                                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }).orElseThrow(() -> new IllegalStateException("Could not find " + statementFile));
                    }
                });

        Map<String, Object> metadataMap = Cypher.toMap(metadata.context().metadata());
        return database.execute(cypher, Map.of("metadata", metadataMap, CommonModel.Entity.id.name(), entityId), 30)
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
                                Class<?> snapshotClass = (Class<?>) ((ParameterizedType) entity.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
                                snapshot.set((T) objectMapper.treeToValue(json, snapshotClass));
                                return false;
                            }
                        });

                        if (snapshot.get() == null) {
                            return CompletableFuture.failedStage(new NotFoundException(metadata.getAggregateId()));
                        }

                        return CompletableFuture.completedStage(snapshot.get());
                    } catch (Throwable t) {
                        return CompletableFuture.failedStage(t);
                    }
                }).toCompletableFuture().join();
    }
}
