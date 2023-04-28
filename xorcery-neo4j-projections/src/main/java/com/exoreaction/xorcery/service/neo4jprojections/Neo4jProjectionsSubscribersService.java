package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.fasterxml.jackson.databind.node.ContainerNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

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
                                              GraphDatabases graphDatabases,
                                              IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                              MetricRegistry metricRegistry) {
        graphDatabase = graphDatabases.apply("neo4j");

        // This service can subscribe to external publishers
        configuration.getObjectListAs("neo4jprojections.subscribers", Publisher::new).ifPresent(publishers ->
        {
            List<Neo4jEventProjection> projectionList = new ArrayList<>();
            neo4jEventProjectionList.forEach(projectionList::add);
            for (Publisher publisher : publishers) {

                final Configuration publisherConfiguration = publisher.getPublisherConfiguration().orElse(Configuration.empty());
                Optional<ProjectionModel> currentProjection = neo4jProjectionsService.getCurrentProjection(publisher.getProjection());
                currentProjection.ifPresent(projectionModel -> publisherConfiguration.json().set("revision", publisherConfiguration.json().numberNode(projectionModel.getRevision().orElse(0L))));

                reactiveStreamsClient.subscribe(publisher.getAuthority(), publisher.getStream(),
                        () ->
                        {
                            // Update to current revision, if available
                            neo4jProjectionsService.getCurrentProjection(publisher.getProjection()).ifPresent(projectionModel -> publisherConfiguration.json().set("revision", publisherConfiguration.json().numberNode(projectionModel.getRevision().orElse(0L))));
                            return publisherConfiguration;
                        },
                        new ProjectionSubscriber(
                                subscription -> new Neo4jProjectionEventHandler(
                                        graphDatabase.getGraphDatabaseService(),
                                        subscription,
                                        currentProjection,
                                        publisher.getProjection(),
                                        neo4jProjectionsService.getNeo4jProjectionCommitPublisher(),
                                        projectionList,
                                        metricRegistry),
                                new DisruptorConfiguration(configuration.getConfiguration("disruptor.standard"))),
                        ProjectionSubscriber.class, Configuration.empty());
            }
        });
    }

    record Publisher(ContainerNode<?> json)
            implements JsonElement {
        String getAuthority() {
            return getString("authority").orElseThrow();
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
