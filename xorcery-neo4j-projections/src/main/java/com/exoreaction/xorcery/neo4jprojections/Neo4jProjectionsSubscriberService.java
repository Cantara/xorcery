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
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.api.WaitForProjectionCommit;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.neo4jprojections.streams.ProjectionWithResultSubscriber;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = "neo4jprojections.eventsubscriber")
@RunLevel(6)
public class Neo4jProjectionsSubscriberService {

    private final Logger logger = LogManager.getLogger(getClass());

    @Inject
    public Neo4jProjectionsSubscriberService(ReactiveStreamsServer reactiveStreamsServer,
                                             Neo4jProjectionsService neo4jProjectionsService,
                                             Configuration configuration,
                                             GraphDatabase graphDatabase,
                                             IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                             MetricRegistry metricRegistry) {

        Neo4jProjectionsConfiguration neo4jProjectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = neo4jProjectionsService.getNeo4jProjectionCommitPublisher();

        List<Neo4jEventProjection> projectionList = new ArrayList<>();
        neo4jEventProjectionList.forEach(projectionList::add);

        reactiveStreamsServer.subscriber("neo4jprojections", cfg ->
                {
                    WaitForProjectionCommit waitForProjectionCommit = new WaitForProjectionCommit(cfg.getString("projection").orElseThrow());
                    neo4jProjectionCommitPublisher.subscribe(waitForProjectionCommit);
                    return new ProjectionWithResultSubscriber(
                            new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                                    graphDatabase.getGraphDatabaseService(),
                                    neo4jProjectionsConfiguration,
                                    subscription,
                                    neo4jProjectionsService.getCurrentProjection(cfg.getString("projection").orElseThrow()),
                                    cfg.getString("projection").orElseThrow(),
                                    neo4jProjectionCommitPublisher,
                                    projectionList,
                                    metricRegistry),
                                    new DisruptorConfiguration(configuration.getConfiguration("disruptor.standard"))),
                            waitForProjectionCommit);
                },
                ProjectionSubscriber.class);

        if (neo4jProjectionsConfiguration.isCommitPublisherEnabled()) {
            reactiveStreamsServer.publisher("neo4jprojectioncommits", cfg -> neo4jProjectionCommitPublisher, Neo4jProjectionCommitPublisher.class);
        }
    }
}
