package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.service.conductor.resources.model.GroupTemplate;

public interface ConductorListener {
    default void addedTemplate(GroupTemplate groupTemplate)
    {

    }

    default void addedGroup(Group group)
    {

    }

    default void updatedGroup(Group group)
    {

    }
}
