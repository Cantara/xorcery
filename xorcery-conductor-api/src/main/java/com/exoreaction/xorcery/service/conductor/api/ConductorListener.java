package com.exoreaction.xorcery.service.conductor.api;

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
