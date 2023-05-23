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
package com.exoreaction.xorcery.neo4jprojections.streams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;

import java.util.Optional;
import java.util.function.Supplier;

public record ProjectionSubscriberFactory(
        Optional<Configuration> publisherConfiguration,
        GraphDatabase graphDatabase,
        String projectionId)
        implements Supplier<Configuration> {
    @Override
    public Configuration get() {
        // Check if we already have written data for this projection before
        return graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.revision)
                .first(row -> row.row().getNumber("projection_revision").longValue()).handle((position, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        LogManager.getLogger(getClass()).error("Error looking up existing projection stream revision", exception);
                    }

                    Configuration config = publisherConfiguration.map(cfg -> new Configuration(cfg.object().deepCopy())).orElseGet(Configuration::empty);

                    if (position != null) {
                        config.json().set("from", config.json().numberNode(position));
//                        neo4jProjectionCommitPublisher.accept();
                    }

                    return config;
                }).toCompletableFuture().join();
    }
}
