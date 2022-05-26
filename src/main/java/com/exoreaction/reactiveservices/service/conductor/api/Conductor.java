package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.service.conductor.resources.model.GroupTemplates;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Groups;
import org.glassfish.jersey.spi.Contract;

@Contract
public interface Conductor {
    // Write

    // Read
    void addConductorListener(ConductorListener listener);

    Groups getGroups();
    GroupTemplates getGroupTemplates();
}
