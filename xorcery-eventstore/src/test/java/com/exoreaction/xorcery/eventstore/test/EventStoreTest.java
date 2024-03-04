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
package com.exoreaction.xorcery.eventstore.test;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.core.LoggerContextFactory;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.eventstore.EventStoreService;
import com.exoreaction.xorcery.net.Sockets;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.logging.log4j.LogManager;
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
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Testcontainers(disabledWithoutDocker = true)
public class EventStoreTest {

    private static Configuration configuration = new Configuration.Builder()
            .with(new StandardConfigurationBuilder()::addTestDefaults)
            .add("jetty.server.http.port", Sockets.nextFreePort())
            .add("jetty.server.ssl.port", Sockets.nextFreePort())
            .add("eventstore.uri", "esdb://localhost:2115?tls=false"
            )
            .build();

    static {
        LoggerContextFactory.initialize(configuration);
    }

    private static Xorcery xorcery;

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreTest.class)))
                    .withExposedService("eventstore", 2115)
                    .withStartupTimeout(Duration.ofSeconds(30));

    @BeforeAll
    public static void setup() throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                LogManager.getLogger().error("Uncaught exception " + t.getName(), e);
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
        JsonMapper mapper = new JsonMapper();
        EventStoreService eventStoreService = xorcery.getServiceLocator().getService(EventStoreService.class);
        EventStoreDBClient client = eventStoreService.getClient();
        try {
            WriteResult result = client.appendToStream("stream1",
                    EventData.builderAsJson(UUID.randomUUID(), "someEventType", mapper.writeValueAsBytes(new EventRecord(1, "foobar"))).build(),
                    EventData.builderAsJson(UUID.randomUUID(),"someEventType", mapper.writeValueAsBytes(new EventRecord(2, "foobar"))).build(),
                    EventData.builderAsJson(UUID.randomUUID(),"someEventType", mapper.writeValueAsBytes(new EventRecord(3, "foobar"))).build()
            ).orTimeout(10, TimeUnit.SECONDS).join();

            LogManager.getLogger().info("Write complete");
        } catch (Exception e) {
            LogManager.getLogger().error("Write failed", e);
        }

        ReadResult readResult = client.readStream("stream1", ReadStreamOptions.get().fromStart().forwards())
                .orTimeout(10, TimeUnit.SECONDS).join();

        List<EventRecord> events = readResult.getEvents().stream().map(re -> {
            try {
                return mapper.readValue(re.getEvent().getEventData(), EventRecord.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        LogManager.getLogger().info(events);
    }

    record EventRecord(int val1, String val2) {
    }
}
