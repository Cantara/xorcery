package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;

public interface ConductorListener {
    default void addedGroup(Group group)
    {

    }
}
