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
package dev.xorcery.domainevents.neo4jprojection.providers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.collections.Element;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.entity.Entity;
import dev.xorcery.domainevents.entity.EntityNotFoundException;
import dev.xorcery.domainevents.publisher.spi.EntitySnapshotProvider;
import dev.xorcery.domainevents.publisher.spi.Snapshot;
import dev.xorcery.json.JsonElement;
import dev.xorcery.neo4j.client.Cypher;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4j.client.RowModel;
import dev.xorcery.util.Resources;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.Result;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static dev.xorcery.lang.Exceptions.unwrap;

@Service
@ContractsProvided(EntitySnapshotProvider.class)
public class Neo4jEntitySnapshotLoader
        implements EntitySnapshotProvider {
    private final ObjectMapper objectMapper;
    private final GraphDatabase database;
    private final Map<CommandEntity, String> cypherCache = new ConcurrentHashMap<>();

    @Inject
    public Neo4jEntitySnapshotLoader(GraphDatabase database) {

        this.database = database;
        this.objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    }

    public CompletableFuture<Snapshot> snapshotFor(CommandMetadata metadata, Command command, Entity entity) {

        String entityId = command.id();
        String cypher = cypherCache.computeIfAbsent(new CommandEntity(command.getClass(), entity.getClass()), this::loadCypher);

        Map<String, Object> metadataMap = Cypher.toMap(metadata.context().metadata());
        return database.execute(cypher, Map.of("metadata", metadataMap, "id", entityId), 30)
                .thenCompose(result ->
                {
                    try (result) {
                        AtomicReference<Snapshot> snapshot = new AtomicReference<>();
                        result.getResult().accept(new Result.ResultVisitor<Exception>() {
                            @Override
                            public boolean visit(Result.ResultRow resultRow) throws Exception {
                                RowModel rowModel = new RowModel(resultRow);
                                Element element = null;
                                long lastUpdatedOn = System.currentTimeMillis();
                                for (String columnName : result.getResult().columns()) {
                                    switch (columnName)
                                    {
                                        case "lastUpdatedOn":{
                                            lastUpdatedOn = resultRow.getNumber(columnName).longValue();
                                            break;
                                        }
                                        case "snapshot":{
                                            JsonNode json = rowModel.getJsonNode(columnName);
                                            element = (JsonElement)()->json;
                                        }
                                    }
                                }
                                if (element == null)
                                    element = Element.empty();
                                snapshot.set(new Snapshot(element, lastUpdatedOn));
                                return false;
                            }
                        });

                        if (snapshot.get() == null) {
                            return CompletableFuture.failedFuture(new EntityNotFoundException(entity.getClass().getSimpleName(), entityId));
                        }

                        return CompletableFuture.completedStage(snapshot.get());
                    } catch (Throwable t) {
                        return CompletableFuture.failedFuture(t);
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> snapshotExists(CommandMetadata metadata, Command command, Entity entity) {
        return snapshotFor(metadata, command, entity)
                .handle((element,throwable) -> throwable == null || !(unwrap(throwable) instanceof EntityNotFoundException));
    }

    private String loadCypher(CommandEntity commandEntity) {
        // Try entity + command first
        String entityCommandFile = "META-INF/neo4j/snapshot/" + commandEntity.entityClass.getSimpleName() + "/" + commandEntity.commandClass.getSimpleName() + ".cyp";
        return Resources.getResource(entityCommandFile).map(url ->
        {
            try (InputStream inputStream = url.openStream()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }).orElseGet(() -> {
            // Try entity only
            String entityFile = "META-INF/neo4j/snapshot/" + commandEntity.entityClass.getSimpleName() + ".cyp";
            return Resources.getResource(entityFile).map(url ->
            {
                try (InputStream inputStream = url.openStream()) {
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).orElseThrow(() -> new IllegalStateException("Could not find Cypher file for " + commandEntity.entityClass.getSimpleName() + "/" + commandEntity.commandClass.getSimpleName()));
        });
    }

    record CommandEntity(Class<? extends Command> commandClass, Class<? extends Entity> entityClass) {
    }
}
