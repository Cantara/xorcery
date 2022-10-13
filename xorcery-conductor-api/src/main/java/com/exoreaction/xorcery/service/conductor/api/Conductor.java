package com.exoreaction.xorcery.service.conductor.api;

import org.glassfish.jersey.spi.Contract;

@Contract
public interface Conductor {
    void addConductorListener(ConductorListener listener);
}
