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
package com.exoreaction.xorcery.eventstore.projections;

import com.eventstore.dbclient.CreateProjectionOptions;
import com.eventstore.dbclient.EventStoreDBProjectionManagementClient;
import com.eventstore.dbclient.ProjectionDetails;
import com.eventstore.dbclient.UpdateProjectionOptions;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.eventstore.EventStoreService;
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

@Service(name = "eventstore.projections")
@RunLevel(4)
public class EventStoreProjectionsService {

    private final Logger logger = LogManager.getLogger(getClass());
    private final EventStoreDBProjectionManagementClient client;

    @Inject
    public EventStoreProjectionsService(Configuration configuration,
                                        EventStoreService eventStoreService) {
        // Create/update projections
        client = EventStoreDBProjectionManagementClient.create(eventStoreService.getSettings());
        List<ProjectionDetails> projectionsList = client.list().join();


        ProjectionsConfiguration cfg = new ProjectionsConfiguration(configuration.getConfiguration("eventstore.projections"));
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
                            logger.error("Could not find projection query " + projection.getQuery());
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
                        // Update
                        exists = true;
                        try {
                            client.update(projectionName, projectionQuery, UpdateProjectionOptions.get()
                                    .emitEnabled(projection.isEmitEnabled())).join();
                            updatedProjections.add(projectionDetails);
                        } catch (Exception e) {
                            logger.error("Could not update projection " + projectionName, e);
                        }
                    }
                }
                if (!exists) {
                    // Create
                    try {
                        client.create(projectionName, projectionQuery, CreateProjectionOptions.get()
                                .emitEnabled(projection.isEmitEnabled())).join();
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
                                    System.out.println("ENABLED:"+details);
                                    client.enable(details.getName()).join();
                                }
                        );

            } else {
                projectionsList.stream()
                        .filter(details -> details.getName().equals(projection.getName()) && details.getStatus().equals("Running"))
                        .findFirst().ifPresent(details ->
                                {
                                    System.out.println("DISABLE:"+details);
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
            } catch (Exception e) {
                logger.error("Could not delete projection " + projectionDetails.getName(), e);
            }
        }
    }

    public EventStoreDBProjectionManagementClient getProjectionManagementClient() {
        return client;
    }


}

