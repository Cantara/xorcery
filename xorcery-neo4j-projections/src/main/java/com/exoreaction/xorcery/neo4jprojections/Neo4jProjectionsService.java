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
package com.exoreaction.xorcery.neo4jprojections;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.neo4jprojections.api.Neo4jProjectionStreams;
import com.exoreaction.xorcery.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = Neo4jProjectionsService.SERVICE_TYPE)
@RunLevel(4)
public class Neo4jProjectionsService {

    public static final String SERVICE_TYPE = "neo4jprojections";

    private final Logger logger = LogManager.getLogger(getClass());
    private final ServiceResourceObject sro;
    private final Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher;
    private final GraphDatabase graphDatabase;

    @Inject
    public Neo4jProjectionsService(ServiceResourceObjects serviceResourceObjects,
                                   Configuration configuration,
                                   GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;

        Neo4jProjectionsConfiguration neo4jProjectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));

        this.sro = new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), SERVICE_TYPE)
                .version(getClass().getPackage().getImplementationVersion())
                .with(b -> {
                    if (neo4jProjectionsConfiguration.isEventSubscriberEnabled())
                        b.subscriber(Neo4jProjectionStreams.EVENT_SUBSCRIBER);
                    if (neo4jProjectionsConfiguration.isCommitPublisherEnabled())
                        b.publisher(Neo4jProjectionStreams.COMMIT_PUBLISHER);
                }).build();

        neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        serviceResourceObjects.add(sro);
    }

    public Optional<ProjectionModel> getCurrentProjection(String projectionId) {
        // Check if we already have written data for this projection before
        return graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.version, Projection.revision)
                .first(row -> row.toModel(ProjectionModel::new, Projection.version, Projection.revision)).handle((model, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        logger.error("Error looking up existing projection details", exception);
                        return Optional.<ProjectionModel>empty();
                    }
                    return Optional.ofNullable(model);
                }).toCompletableFuture().join();
    }

    public Neo4jProjectionCommitPublisher getNeo4jProjectionCommitPublisher() {
        return neo4jProjectionCommitPublisher;
    }
}
