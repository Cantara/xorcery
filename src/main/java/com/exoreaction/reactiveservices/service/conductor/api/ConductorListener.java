package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.service.conductor.resources.model.GroupTemplate;
import com.exoreaction.reactiveservices.service.registry.api.Registry;

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
