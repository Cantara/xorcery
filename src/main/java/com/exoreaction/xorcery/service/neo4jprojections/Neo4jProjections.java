package com.exoreaction.xorcery.service.neo4jprojections;

import org.glassfish.jersey.spi.Contract;

@Contract
public interface Neo4jProjections {
    void addProjectionListener(ProjectionListener listener);
}
