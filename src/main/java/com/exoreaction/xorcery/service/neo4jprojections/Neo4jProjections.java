package com.exoreaction.xorcery.service.neo4jprojections;

import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.CompletableFuture;

@Contract
public interface Neo4jProjections {
    void addProjectionListener(ProjectionListener listener);

    CompletableFuture<Void> isLive(String projectionId);
}
