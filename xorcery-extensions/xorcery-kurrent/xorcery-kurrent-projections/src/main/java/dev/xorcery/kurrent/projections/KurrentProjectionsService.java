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
package dev.xorcery.kurrent.projections;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.kurrent.client.api.KurrentClients;
import io.kurrent.dbclient.CreateProjectionOptions;
import io.kurrent.dbclient.KurrentDBProjectionManagementClient;
import io.kurrent.dbclient.ProjectionDetails;
import io.kurrent.dbclient.UpdateProjectionOptions;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service(name = "kurrent.projections")
@RunLevel(4)
public class KurrentProjectionsService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final KurrentDBProjectionManagementClient client;

    @Inject
    public KurrentProjectionsService(Configuration configuration,
                                     KurrentClients kurrentClients) {
        // Create/update projections
        client = KurrentDBProjectionManagementClient.from(kurrentClients.getDefaultClient().getClient());
        List<ProjectionDetails> projectionsList = client.list().join();


        ProjectionsConfiguration cfg = new ProjectionsConfiguration(configuration.getConfiguration("kurrent.projections"));
        List<ProjectionDetails> updatedProjections = new ArrayList<>();
        for (Projection projection : cfg.getProjections()) {

            String projectionName = projection.getName();

            projection.getQuery().ifPresent(file ->
            {
                logger.info("Loading projection:" + projectionName);

                String projectionQuery = null;
                try {
                    URI queryUri = URI.create(file);
                    projectionQuery = new String(queryUri.toURL().openStream().readAllBytes(), StandardCharsets.UTF_8);
                } catch (IllegalArgumentException | IOException e) {
                    // Just load from classpath
                    try (InputStream in = ClassLoader.getSystemResourceAsStream(file)) {
                        if (in == null) {
                            logger.info("Could not find projection query " + projection.getQuery());
                        } else {
                            projectionQuery = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        }
                    } catch (IOException ex) {
                        logger.error("Could not load projection query " + projectionName, ex);
                    }
                }
                if (projectionQuery == null)
                    return;

                boolean exists = false;
                for (ProjectionDetails projectionDetails : projectionsList) {
                    if (projectionDetails.getName().equals(projectionName)) {
                        exists = true;
                        updatedProjections.add(projectionDetails);
                        if (projectionDetails.getVersion() < projection.getVersion()){
                            // Update if new version
                            try {
                                client.update(projectionName, projectionQuery, UpdateProjectionOptions.get()
                                        .emitEnabled(projection.isEmitEnabled())).join();
                                logger.info("Updated projection " + projectionName);
                            } catch (Exception e) {
                                logger.error("Could not update projection " + projectionName, e);
                            }
                        } else {
                            logger.info("Projection query has not been changed:" + projectionName);
                        }
                    }
                }
                if (!exists) {
                    // Create
                    try {
                        client.create(projectionName, projectionQuery,
                                CreateProjectionOptions.get()
                                        .emitEnabled(projection.isEmitEnabled())
                                        .trackEmittedStreams(projection.isTrackingEnabled())).join();
                        logger.info("Created projection " + projectionName);

                        if (projection.isEnabled()) {
                            client.enable(projectionName).join();
                        }
                    } catch (Exception e) {
                        logger.error("Could not create projection " + projectionName, e);
                    }
                }
            });

            if (projection.isEnabled()) {
                projectionsList.stream()
                        .filter(details -> details.getName().equals(projection.getName()) && !details.getStatus().equals("Running"))
                        .findFirst().ifPresent(details ->
                                {
                                    logger.info("Enabled projection " + projectionName);
                                    client.enable(details.getName()).join();
                                }
                        );

            } else {
                projectionsList.stream()
                        .filter(details -> details.getName().equals(projection.getName()) && details.getStatus().equals("Running"))
                        .findFirst().ifPresent(details ->
                                {
                                    logger.info("Disabled projection " + details.getName());
                                    client.disable(details.getName()).join();
                                }
                        );
            }
        }
        projectionsList.removeAll(updatedProjections);

        // Delete remaining
        for (ProjectionDetails projectionDetails : projectionsList) {
            if (projectionDetails.getName().startsWith("$")) {
                // System projection, ignore
                continue;
            }
            try {
                client.disable(projectionDetails.getName()).join();
                client.delete(projectionDetails.getName()).join();
                logger.info("Deleted projection " + projectionDetails.getName());
            } catch (Exception e) {
                logger.error("Could not delete projection " + projectionDetails.getName(), e);
            }
        }
    }

    public KurrentDBProjectionManagementClient getProjectionManagementClient() {
        return client;
    }
}

