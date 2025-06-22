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
package dev.xorcery.kurrent.projections.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.kurrent.client.api.KurrentClients;
import dev.xorcery.kurrent.projections.KurrentProjectionsService;
import io.kurrent.dbclient.EventDataBuilder;
import io.kurrent.dbclient.ProjectionDetails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.time.Duration;
import java.util.List;

@Testcontainers(disabledWithoutDocker = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class KurrentProjectionsTest {

    private final Logger logger = LogManager.getLogger(getClass());

    @Container
    public static DockerComposeContainer environment =
            new DockerComposeContainer(new File("src/test/resources/docker-compose.yaml"))
                    .withLogConsumer("kurrent", new Slf4jLogConsumer(LoggerFactory.getLogger(KurrentProjectionsTest.class)))
                    .withExposedService("kurrent", 2115)
                    .withStartupTimeout(Duration.ofMinutes(10));

    @Test
    @Order(1)
    public void createProjection() throws Exception {
        String config = """
                kurrent:
                    projections:
                        projections:
                            - name: testprojection
                              query: testprojections.js
                              version: 0
                            - name: $by_category
                              enabled: true
                """;

        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();
        logger.info(configuration);
        try (Xorcery xorcery = new Xorcery(configuration)) {
            KurrentProjectionsService projectionsService = xorcery.getServiceLocator().getService(KurrentProjectionsService.class);

            List<ProjectionDetails> projectionDetails = projectionsService.getProjectionManagementClient().list().join();
            ObjectMapper mapper = new ObjectMapper();
            for (ProjectionDetails projectionDetail : projectionDetails) {
                System.out.println(mapper.writeValueAsString(projectionDetail));
            }

            // Write event
            KurrentClients kurrentClients = xorcery.getServiceLocator().getService(KurrentClients.class);
            kurrentClients.getDefaultClient().getClient().appendToStream("teststream", EventDataBuilder.json("testtype", "{}".getBytes()).build()).join();

            // Check projection state
            CountState state = projectionsService.getProjectionManagementClient().getState("testprojection", CountState.class).join();

            System.out.println("Count:" + state.count());

            List<ProjectionDetails> projections = projectionsService.getProjectionManagementClient().list().join();
            for (ProjectionDetails projection : projections) {
                System.out.println(projection);
            }
        }
    }

    @Test
    @Order(2)
    public void updateProjection() throws Exception {
        String config = """
                kurrent:
                    projections:
                        projections:
                            - name: testprojection
                              query: testprojectionsv2.js
                              version: 1
                """;

        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();
        logger.info(configuration);
        try (Xorcery xorcery = new Xorcery(configuration)) {
            KurrentProjectionsService projectionsService = xorcery.getServiceLocator().getService(KurrentProjectionsService.class);

            List<ProjectionDetails> projectionDetails = projectionsService.getProjectionManagementClient().list().join();
            ObjectMapper mapper = new ObjectMapper();
            for (ProjectionDetails projectionDetail : projectionDetails) {
                System.out.println(mapper.writeValueAsString(projectionDetail));
            }

            // Write event
            KurrentClients kurrentClients = xorcery.getServiceLocator().getService(KurrentClients.class);
            kurrentClients.getDefaultClient().getClient().appendToStream("teststream", EventDataBuilder.json("testtype", "{}".getBytes()).build()).join();

            // Check projection state
            CountState state = projectionsService.getProjectionManagementClient().getState("testprojection", CountState.class).join();

            System.out.println("Count:" + state.count());

            List<ProjectionDetails> projections = projectionsService.getProjectionManagementClient().list().join();
            for (ProjectionDetails projection : projections) {
                System.out.println(projection);
            }
        }
    }

    @Test
    @Order(3)
    public void unchangedProjection() throws Exception {
        String config = """
                kurrent:
                    projections:
                        projections:
                            - name: testprojection
                              query: testprojectionsv2.js
                              version: 1
                """;

        Configuration configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();
        logger.info(configuration);
        try (Xorcery xorcery = new Xorcery(configuration)) {
            KurrentProjectionsService projectionsService = xorcery.getServiceLocator().getService(KurrentProjectionsService.class);

            List<ProjectionDetails> projectionDetails = projectionsService.getProjectionManagementClient().list().join();
            ObjectMapper mapper = new ObjectMapper();
            for (ProjectionDetails projectionDetail : projectionDetails) {
                System.out.println(mapper.writeValueAsString(projectionDetail));
            }

            // Write event
            KurrentClients kurrentClients = xorcery.getServiceLocator().getService(KurrentClients.class);
            kurrentClients.getDefaultClient().getClient().appendToStream("teststream", EventDataBuilder.json("testtype", "{}".getBytes()).build()).join();

            // Check projection state
            CountState state = projectionsService.getProjectionManagementClient().getState("testprojection", CountState.class).join();

            System.out.println("Count:" + state.count());

            List<ProjectionDetails> projections = projectionsService.getProjectionManagementClient().list().join();
            for (ProjectionDetails projection : projections) {
                System.out.println(projection);
            }
        }
    }

    public record CountState(int count) {
    }
}
