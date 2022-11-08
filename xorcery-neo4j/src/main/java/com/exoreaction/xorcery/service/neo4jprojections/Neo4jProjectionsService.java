package com.exoreaction.xorcery.service.neo4jprojections;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.TopicSubscribers;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabases;
import com.exoreaction.xorcery.service.neo4jprojections.api.Neo4jProjectionRels;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionEventHandler;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriber;
import com.exoreaction.xorcery.service.neo4jprojections.streams.ProjectionSubscriberGroupListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Service(name = Neo4jProjectionsService.SERVICE_TYPE)
public class Neo4jProjectionsService {

    public static final String SERVICE_TYPE = "neo4jprojections";

    private final Logger logger = LogManager.getLogger(getClass());
    private final ServiceResourceObject sro;

    @Inject
    public Neo4jProjectionsService(ServiceResourceObjects serviceResourceObjects,
                                   ServiceLocator serviceLocator,
                                   ReactiveStreams reactiveStreams,
                                   Configuration configuration,
                                   GraphDatabases graphDatabases,
                                   IterableProvider<Neo4jEventProjection> neo4jEventProjectionList,
                                   MetricRegistry metricRegistry) {
        this.sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .with(b -> {
                    if (configuration.getBoolean("neo4jprojections.subscriber").orElse(true))
                        b.websocket(Neo4jProjectionRels.neo4jprojectionssubscriber.name(), "ws/neo4jprojections/subscriber");
                    if (configuration.getBoolean("neo4jprojections.publisher").orElse(true))
                        b.websocket(Neo4jProjectionRels.neo4jprojectionspublisher.name(), "ws/neo4jprojections/publisher");
                }).build();

        Neo4jProjectionCommitPublisher neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        sro.getLinkByRel(Neo4jProjectionRels.neo4jprojectionssubscriber.name()).ifPresent(link ->
        {
            List<Neo4jEventProjection> projectionList = new ArrayList<>();
            neo4jEventProjectionList.forEach(projectionList::add);

            reactiveStreams.subscriber(link.getHrefAsUri().getPath(), cfg -> new ProjectionSubscriber(subscription -> new Neo4jProjectionEventHandler(
                    graphDatabases.apply(cfg.getString("database").orElse("neo4j")).getGraphDatabaseService(),
                    subscription,
                    Optional.empty(),
                    cfg,
                    neo4jProjectionCommitPublisher,
                    projectionList,
                    metricRegistry)), ProjectionSubscriber.class);

            TopicSubscribers.addSubscriber(serviceLocator, new ProjectionSubscriberGroupListener(graphDatabases,
                    reactiveStreams, sro.getServiceIdentifier(), projectionList, metricRegistry, neo4jProjectionCommitPublisher));
        });

        sro.getLinkByRel(Neo4jProjectionRels.neo4jprojectionspublisher.name()).ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> neo4jProjectionCommitPublisher, Neo4jProjectionCommitPublisher.class);
        });

        serviceResourceObjects.publish(sro);
    }
}
