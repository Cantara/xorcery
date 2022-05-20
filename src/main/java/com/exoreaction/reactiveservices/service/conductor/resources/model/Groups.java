package com.exoreaction.reactiveservices.service.conductor.resources.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Groups {

    public interface GroupsListener {
        void addedGroup(Group group);
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

    public List<Group> getGroups() {
        return groups;
    }

}
