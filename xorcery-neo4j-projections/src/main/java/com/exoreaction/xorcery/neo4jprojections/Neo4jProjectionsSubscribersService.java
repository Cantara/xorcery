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

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.fasterxml.jackson.databind.node.ContainerNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = "neo4jprojections")
@RunLevel(8)
public class Neo4jProjectionsSubscribersService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final GraphDatabase graphDatabase;

    @Inject
    public Neo4jProjectionsSubscribersService(Neo4jProjectionsService neo4jProjectionsService,
                                              ReactiveStreamsClient reactiveStreamsClient,
                                              Configuration configuration,
                                              GraphDatabase graphDatabase,
                                              IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                              MetricRegistry metricRegistry) {
        this.graphDatabase = graphDatabase;
        Neo4jProjectionsConfiguration neo4jProjectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));

        // This service can subscribe to external publishers
        configuration.getObjectListAs("neo4jprojections.subscribers", Publisher::new).ifPresent(publishers ->
        {
            List<Neo4jEventProjection> projectionList = new ArrayList<>();
            neo4jEventProjectionList.forEach(projectionList::add);
            for (Publisher publisher : publishers) {

                final Configuration publisherConfiguration = publisher.getPublisherConfiguration().orElse(Configuration.empty());
                Optional<ProjectionModel> currentProjection = neo4jProjectionsService.getCurrentProjection(publisher.getProjection());
                currentProjection.ifPresent(projectionModel -> publisherConfiguration.json().set("revision", publisherConfiguration.json().numberNode(projectionModel.getRevision().orElse(0L))));

                reactiveStreamsClient.subscribe(publisher.getURI().orElse(null), publisher.getStream(),
                        () ->
                        {
                            // Update to current revision, if available
                            neo4jProjectionsService.getCurrentProjection(publisher.getProjection()).ifPresent(projectionModel -> publisherConfiguration.json().set("revision", publisherConfiguration.json().numberNode(projectionModel.getRevision().orElse(0L))));
                            return publisherConfiguration;
                        },
                        new ProjectionSubscriber(
                                subscription -> new Neo4jProjectionEventHandler(
                                        graphDatabase.getGraphDatabaseService(),
                                        neo4jProjectionsConfiguration,
                                        subscription,
                                        currentProjection,
                                        publisher.getProjection(),
                                        neo4jProjectionsService.getNeo4jProjectionCommitPublisher(),
                                        projectionList,
                                        metricRegistry),
                                new DisruptorConfiguration(configuration.getConfiguration("disruptor.standard"))),
                        ProjectionSubscriber.class, ClientConfiguration.defaults());
            }
        });
    }

    record Publisher(ContainerNode<?> json)
            implements JsonElement {
        Optional<URI> getURI() {
            return JsonElement.super.getURI("uri");
        }

        String getStream() {
            return getString("stream").orElseThrow();
        }

        String getProjection() {
            return getString("projection").orElseThrow();
        }

        Optional<Configuration> getPublisherConfiguration() {
            return getObjectAs("configuration", Configuration::new);
        }
    }
}
