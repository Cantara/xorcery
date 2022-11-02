package com.exoreaction.xorcery.service.eventstore.test;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Testcontainers
@Disabled
public class EventStoreIT {

    private final Logger logger = LogManager.getLogger(getClass());

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreIT.class)))
                    .withExposedService("eventstore", 2113);

    @Disabled
    @Test
    public void testReadBackwards() throws IOException, ParseError, ExecutionException, InterruptedException, TimeoutException {
        Configuration configuration = new Configuration.Builder().with(new StandardConfigurationBuilder()::addTestDefaults).build();

        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        EventStoreDBClient client = EventStoreDBClient.create(settings);

        List<ResolvedEvent> lastEvent = client.readStream("development-default-forum", 1, ReadStreamOptions.get().backwards().fromEnd())
                .get(10, TimeUnit.SECONDS).getEvents();

        for (ResolvedEvent resolvedEvent : lastEvent) {
            System.out.println(resolvedEvent.getEvent().getStreamRevision().getValueUnsigned());
        }
    }

    @Test
    public void createProjection() throws Exception {
        String config = """
                eventstore:
                    publisher.enabled: false
                    subscriber.enabled: false
                    projections:
                        projections:
                            - name: testprojection
                              query: META-INF/testprojection.js
                """;

        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration = new Configuration.Builder()
                .with(standardConfigurationBuilder.addTestDefaultsWithYaml(config))
                .build();
        logger.info(standardConfigurationBuilder.toYaml(configuration));
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            EventStoreService eventStoreService = xorcery.getServiceLocator().getService(EventStoreService.class);

            List<ProjectionDetails> projectionDetails = eventStoreService.getProjectionManagementClient().list().join().getProjections();
            ObjectMapper mapper = new ObjectMapper();
            for (ProjectionDetails projectionDetail : projectionDetails) {
                System.out.println(mapper.writeValueAsString(projectionDetail));
            }

            // Write event
            eventStoreService.getClient().appendToStream("teststream", EventDataBuilder.json("testtype", "{}").build()).join();

            // Check projection state
            CountState state = eventStoreService.getProjectionManagementClient().getState("testprojection", CountState.class).join();

            System.out.println("Count:"+state.count());
        }

    }

    public record CountState(int count)
    {
    }
}
