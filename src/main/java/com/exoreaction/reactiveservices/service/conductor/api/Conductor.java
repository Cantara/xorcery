package com.exoreaction.reactiveservices.service.conductor.api;

import org.glassfish.jersey.spi.Contract;

@Contract
public interface Conductor {
    // Write

    // Read
    void addConductorListener(ConductorListener listener);
}
