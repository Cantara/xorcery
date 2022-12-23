package com.exoreaction.xorcery.service.neo4jprojections.streams;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4jprojections.Projection;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;

import java.util.Optional;
import java.util.function.Supplier;

public record ProjectionSubscriberFactory(
        Optional<Configuration> publisherConfiguration,
        GraphDatabase graphDatabase,
        String projectionId)
        implements Supplier<Configuration> {
    @Override
    public Configuration get() {
        // Check if we already have written data for this projection before
        return graphDatabase.query("MATCH (Projection:Projection {id:$projection_id})")
                .parameter(Projection.id, projectionId)
                .results(Projection.revision)
                .first(row -> row.row().getNumber("projection_revision").longValue()).handle((position, exception) ->
                {
                    if (exception != null && !(exception.getCause() instanceof NotFoundException)) {
                        LogManager.getLogger(getClass()).error("Error looking up existing projection stream revision", exception);
                    }

                    Configuration config = publisherConfiguration.map(cfg -> new Configuration(cfg.object().deepCopy())).orElseGet(Configuration::empty);

                    if (position != null) {
                        config.json().set("from", config.json().numberNode(position));
//                        neo4jProjectionCommitPublisher.accept();
                    }

                    return config;
                }).toCompletableFuture().join();
    }
}
