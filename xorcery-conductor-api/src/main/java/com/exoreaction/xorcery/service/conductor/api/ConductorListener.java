package com.exoreaction.xorcery.service.conductor.api;

import com.exoreaction.xorcery.service.conductor.resources.model.Group;
import com.exoreaction.xorcery.service.conductor.resources.model.GroupTemplate;
import com.exoreaction.xorcery.service.registry.api.Registry;

public interface ConductorListener {
    default void addedTemplate(GroupTemplate groupTemplate)
    {

    }

    default void addedGroup(Group group, Registry registry)
    {

    }

    default void updatedGroup(Group group, Registry registry)
    {

    }
}
