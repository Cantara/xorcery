package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.CreateProjectionOptions;
import com.eventstore.dbclient.EventStoreDBProjectionManagementClient;
import com.eventstore.dbclient.ProjectionDetails;
import com.eventstore.dbclient.UpdateProjectionOptions;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Named("eventstore.projections")
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
            logger.info("Loading projection:" + projectionName);

            String projectionQuery = null;
            try {
                URI queryUri = URI.create(projection.getQuery());
                projectionQuery = new String(queryUri.toURL().openStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException | IOException e) {
                // Just load from classpath
                try (InputStream in = ClassLoader.getSystemResourceAsStream(projection.getQuery())) {
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
                continue;

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
        }
        projectionsList.removeAll(updatedProjections);

        // Delete remaining
        for (ProjectionDetails projectionDetails : projectionsList) {
            if (projectionDetails.getName().startsWith("$")) {
                // System projection, ignore
                continue;
            }
            try {
                client.delete(projectionDetails.getName()).join();
            } catch (Exception e) {
                logger.error("Could not delete projection " + projectionDetails.getName(), e);
            }
        }
    }

    public EventStoreDBProjectionManagementClient getProjectionManagementClient() {
        return client;
    }


    public record ProjectionsConfiguration(Configuration context)
            implements ServiceConfiguration {
        public Iterable<Projection> getProjections() {
            return context.getObjectListAs("projections", Projection::new).orElseThrow(() ->
                    new IllegalStateException("Missing eventstore.projections.projections configuration"));
        }
    }

    public record Projection(ObjectNode json)
            implements JsonElement {
        String getName() {
            return getString("name").orElseThrow();
        }

        String getQuery() {
            return getString("query").orElseThrow();
        }

        public boolean isEmitEnabled() {
            return getBoolean("emitenabled").orElse(false);
        }
    }
}

