package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class Groups {

    public interface GroupsListener {
        default void addedGroup(Group group) {
        }

        default void updatedGroup(Group group) {
        }
    }

    private final List<Group> groups = new CopyOnWriteArrayList<>();
    private final GroupsListener groupsListener;

    public Groups(GroupsListener groupsListener) {
        this.groupsListener = groupsListener;
    }

    public void addGroup(Group addedGroup) {
        groups.add(addedGroup);
        groupsListener.addedGroup(addedGroup);
    }

    public void addService(Group group, ServiceResourceObject serviceResourceObject, ResourceObject settings) {
        if (groups.remove(group))
        {
            Group updatedGroup = group.add(serviceResourceObject, settings);
            groups.add(updatedGroup);
            groupsListener.updatedGroup(updatedGroup);
        }
    }

    public void addService(Group group, ServiceResourceObject serviceResourceObject) {
        if (groups.remove(group))
        {
            Group updatedGroup = group.add(serviceResourceObject);
            groups.add(updatedGroup);
            groupsListener.updatedGroup(updatedGroup);
        }
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Optional<Group> groupByTemplate(GroupTemplate groupTemplate) {
        return getGroups().stream()
                .filter(group -> group.isTemplate(groupTemplate))
                .findFirst();
    }

}
