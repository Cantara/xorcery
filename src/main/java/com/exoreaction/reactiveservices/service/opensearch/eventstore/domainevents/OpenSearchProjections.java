package com.exoreaction.reactiveservices.service.opensearch.eventstore.domainevents;

import org.glassfish.jersey.spi.Contract;

@Contract
public interface OpenSearchProjections {
    void addProjectionListener(ProjectionListener listener);
}
