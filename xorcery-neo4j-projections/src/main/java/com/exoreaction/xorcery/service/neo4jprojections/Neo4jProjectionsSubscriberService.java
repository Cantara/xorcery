package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import jakarta.inject.Inject;
import jakarta.inject.Named;
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
                                             GraphDatabases graphDatabases,
                                             IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                             MetricRegistry metricRegistry) {
        GraphDatabase graphDatabase = graphDatabases.apply("neo4j");

        Neo4jProjectionsConfiguration neo4jProjectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = neo4jProjectionsService.getNeo4jProjectionCommitPublisher();

        List<Neo4jEventProjection> projectionList = new ArrayList<>();
        neo4jEventProjectionList.forEach(projectionList::add);

        reactiveStreamsServer.subscriber("neo4jprojections", cfg -> new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                graphDatabases.apply(cfg.getString("database").orElse("neo4j")).getGraphDatabaseService(),
                subscription,
                neo4jProjectionsService.getCurrentProjection(cfg.getString("projection").orElseThrow()),
                cfg.getString("projection").orElseThrow(),
                neo4jProjectionCommitPublisher,
                projectionList,
                metricRegistry), new DisruptorConfiguration(configuration.getConfiguration("disruptor.standard"))), ProjectionSubscriber.class);

        if (neo4jProjectionsConfiguration.isCommitPublisherEnabled()) {
            reactiveStreamsServer.publisher("neo4jprojectioncommits", cfg -> neo4jProjectionCommitPublisher, Neo4jProjectionCommitPublisher.class);
        }
    }
}
