package com.exoreaction.xorcery.service.conductor.resources.model;

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

    private final CopyOnWriteArrayList<Group> groups = new CopyOnWriteArrayList<>();
    private final GroupsListener groupsListener;

    public Groups(GroupsListener groupsListener) {
        this.groupsListener = groupsListener;
    }

    public void addOrUpdateGroup(Group group) {
        if (groups.addIfAbsent(group)) {
            groupsListener.addedGroup(group);
        } else {
            groups.remove(group);
            groups.add(group);
            groupsListener.updatedGroup(group);
        }
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Optional<Group> getGroupByTemplate(GroupTemplate groupTemplate) {
        return getGroups().stream()
                .filter(group -> group.isTemplate(groupTemplate))
                .findFirst();
    }

}
