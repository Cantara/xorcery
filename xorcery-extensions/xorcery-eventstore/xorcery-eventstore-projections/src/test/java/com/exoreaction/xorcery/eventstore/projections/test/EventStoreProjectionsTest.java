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
package com.exoreaction.xorcery.eventstore.projections.test;

import com.eventstore.dbclient.EventDataBuilder;
import com.eventstore.dbclient.ProjectionDetails;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.eventstore.EventStoreService;
import com.exoreaction.xorcery.eventstore.projections.EventStoreProjectionsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.List;

@Testcontainers(disabledWithoutDocker = true)
public class EventStoreProjectionsTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/compose-test.yaml"))
                    .withLogConsumer("eventstore", new Slf4jLogConsumer(LoggerFactory.getLogger(EventStoreProjectionsTest.class)))
                    .withExposedService("eventstore", 2115)
                    .withStartupTimeout(Duration.ofMinutes(10));

    @Test
    public void createProjection() throws Exception {
        String config = """
                eventstore:
                    uri: "esdb://localhost:2115?tls=false"
                    streams.enabled: false
                    projections:
                        projections:
                            - name: testprojection
                              query: testprojections.js
                """;

        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();
        logger.info(configuration);
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
