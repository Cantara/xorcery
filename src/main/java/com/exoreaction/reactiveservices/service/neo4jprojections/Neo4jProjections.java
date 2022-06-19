package com.exoreaction.reactiveservices.service.neo4jprojections;

import org.glassfish.jersey.spi.Contract;

@Contract
public interface Neo4jProjections {
    void addProjectionListener(ProjectionListener listener);
}
