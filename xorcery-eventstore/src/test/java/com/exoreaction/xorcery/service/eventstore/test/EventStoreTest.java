package com.exoreaction.xorcery.service.eventstore.test;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.WriteResult;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Testcontainers(disabledWithoutDocker = true)
public class EventStoreTest {

    private static Xorcery xorcery;
    private static final Logger logger = LogManager.getLogger(EventStoreTest.class);

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreTest.class)))
                    .withExposedService("eventstore", 2113);

    @BeforeAll
    public static void setup() throws Exception {
        Configuration configuration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder()::addTestDefaults)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        logger.info(StandardConfigurationBuilder.toYaml(configuration));
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                logger.error("Uncaught exception "+t.getName(), e);
            }
        });
        xorcery = new Xorcery(configuration);
    }

    @AfterAll
    public static void shutdown() {
        xorcery.close();
    }


    @Test
    public void testWriteAndReadEvents() throws IOException {
        EventStoreService eventStoreService = xorcery.getServiceLocator().getService(EventStoreService.class);
        WriteResult result = eventStoreService.getClient().appendToStream("stream1",
                EventData.builderAsJson("someEventType", new EventRecord(1, "foobar")).build(),
                EventData.builderAsJson("someEventType", new EventRecord(2, "foobar")).build(),
                EventData.builderAsJson("someEventType", new EventRecord(3, "foobar")).build()
        ).join();

        logger.info("Write complete");

        ReadResult readResult = eventStoreService.getClient().readStream("stream1", ReadStreamOptions.get().fromStart().forwards()).join();

        List<EventRecord> events = readResult.getEvents().stream().map(re -> {
            try {
                return re.getEvent().getEventDataAs(EventRecord.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        logger.info(events);
    }

    record EventRecord(int val1, String val2) {
    }
}
