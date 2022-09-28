package com.exoreaction.xorcery.service.conductor.api;

import com.exoreaction.xorcery.service.conductor.resources.model.GroupTemplates;
import com.exoreaction.xorcery.service.conductor.resources.model.Groups;
import org.glassfish.jersey.spi.Contract;

@Contract
public interface Conductor {
    // Write

    // Read
    void addConductorListener(ConductorListener listener);

    Groups getGroups();
    GroupTemplates getGroupTemplates();
}
