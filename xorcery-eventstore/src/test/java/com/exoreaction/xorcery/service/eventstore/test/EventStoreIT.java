package com.exoreaction.xorcery.service.eventstore.test;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;
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
public class EventStoreIT {

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
        LogManager.getLogger(getClass()).info("CREATE PROJECTION TEST");
        System.out.println("Starting test");

/*
        String config = """
                eventstore:
                    projections:
                        projections:
                            - name: testprojection
                              query: META-INF/testprojection.js
                """;

        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .build();
        StandardConfiguration standardConfiguration = () -> configuration;
        try (Xorcery xorcery = new Xorcery(configuration)) {
            EventStoreService eventStoreService = xorcery.getServiceLocator().getService(EventStoreService.class);

            List<ProjectionDetails> projectionDetails = eventStoreService.getProjectionManagementClient().list().join().getProjections();
            ObjectMapper mapper = new ObjectMapper();
            for (ProjectionDetails projectionDetail : projectionDetails) {
                System.out.println(mapper.writeValueAsString(projectionDetail));
            }
        }
*/
    }
}
