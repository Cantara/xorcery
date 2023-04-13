package com.exoreaction.xorcery.service.eventstore.test;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.eventstore.projections.EventStoreProjectionsService;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.File;
import java.util.List;

//@Testcontainers(disabledWithoutDocker = true)
public class EventStoreProjectionsTest {

    private final Logger logger = LogManager.getLogger(getClass());

    //@Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreProjectionsTest.class)))
                    .withExposedService("eventstore", 2113);

    @Test
    @Disabled // TODO Enable test again after fixing bind failed on port 8080 already in use. Preferable should we start all services on ephemeral or high free ports
    public void createProjection() throws Exception {
        String config = """
                eventstore:
                    streams.enabled: false
                    projections:
                        projections:
                            - name: testprojection
                              query: META-INF/testprojection.js
                """;

        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration = new Configuration.Builder()
                .with(standardConfigurationBuilder.addTestDefaultsWithYaml(config))
                .build();
        logger.info(StandardConfigurationBuilder.toYaml(configuration));
        try (Xorcery xorcery = new Xorcery(configuration)) {
            EventStoreProjectionsService projectionsService = xorcery.getServiceLocator().getService(EventStoreProjectionsService.class);

            List<ProjectionDetails> projectionDetails = projectionsService.getProjectionManagementClient().list().join();
            ObjectMapper mapper = new ObjectMapper();
            for (ProjectionDetails projectionDetail : projectionDetails) {
                System.out.println(mapper.writeValueAsString(projectionDetail));
            }

            // Write event
            EventStoreService eventStoreService = xorcery.getServiceLocator().getService(EventStoreService.class);
            eventStoreService.getClient().appendToStream("teststream", EventDataBuilder.json("testtype", "{}").build()).join();

            // Check projection state
            CountState state = projectionsService.getProjectionManagementClient().getState("testprojection", CountState.class).join();

            System.out.println("Count:" + state.count());
        }
    }

    public record CountState(int count) {
    }
}
