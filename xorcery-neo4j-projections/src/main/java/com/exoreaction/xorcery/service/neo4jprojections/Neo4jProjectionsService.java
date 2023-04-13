package com.exoreaction.xorcery.service.neo4jprojections;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.neo4jprojections.api.Neo4jProjectionStreams;
import com.exoreaction.xorcery.service.neo4jprojections.streams.Neo4jProjectionCommitPublisher;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

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

    @Inject
    public Neo4jProjectionsService(ServiceResourceObjects serviceResourceObjects,
                                   Configuration configuration) {

        Neo4jProjectionsConfiguration neo4jProjectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));

        this.sro = new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), SERVICE_TYPE)
                .with(b -> {
                    if (neo4jProjectionsConfiguration.isEventSubscriberEnabled())
                        b.subscriber(Neo4jProjectionStreams.EVENT_SUBSCRIBER);
                    if (neo4jProjectionsConfiguration.isCommitPublisherEnabled())
                        b.publisher(Neo4jProjectionStreams.COMMIT_PUBLISHER);
                }).build();

        neo4jProjectionCommitPublisher = new Neo4jProjectionCommitPublisher();

        serviceResourceObjects.add(sro);
    }

    public Neo4jProjectionCommitPublisher getNeo4jProjectionCommitPublisher() {
        return neo4jProjectionCommitPublisher;
    }
}
