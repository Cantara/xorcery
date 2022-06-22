package com.exoreaction.xorcery.service.conductor.resources.model;

import com.exoreaction.xorcery.jsonapi.model.Relationship;
import com.exoreaction.xorcery.jsonapi.model.Relationships;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupTemplates {

    public interface GroupTemplatesListener {
        default void addedTemplate(GroupTemplate groupTemplate) {
        }

        default void createdPartialGroup(Group group) {
        }
    }

    public GroupTemplates(GroupTemplatesListener listener, Groups groups) {
        this.listener = listener;
        this.groups = groups;
    }

    private final GroupTemplatesListener listener;
    private final Groups groups;

    private final List<GroupTemplate> groupTemplates = new CopyOnWriteArrayList<>();
    private final List<String> matchedTemplates = new CopyOnWriteArrayList<>();
    private final Map<String, Group> partialGroups = new ConcurrentHashMap<>();

    public List<GroupTemplate> getTemplates() {
        return groupTemplates;
    }

    public Map<String, Group> getPartialGroups() {
        return partialGroups;
    }

    public void addTemplate(GroupTemplate groupTemplate) {
        groupTemplates.add(groupTemplate);

        listener.addedTemplate(groupTemplate);
    }

    public void addedService(ServiceResourceObject service) {
        try {
            // Check if any group templates match
            // TODO This algo can be simplified
            for (GroupTemplate groupTemplate : groupTemplates) {
                if (groupTemplate.getSources().isSource(service)) {
                    // Check if group exists already
                    groups.getGroupByTemplate(groupTemplate)
                            .ifPresentOrElse(existingGroup ->
                            {
                                // Check if many cardinality
                                if (groupTemplate.getSources().isMany()) {
                                    groups.addOrUpdateGroup(existingGroup.addSource(service));
                                }
                            }, () ->
                            {
                                // Create partial group
                                Group partialGroup = partialGroups.computeIfAbsent(groupTemplate.resourceObject().getId(), templateId ->
                                        new Group(new ResourceObject.Builder("group", templateId)
                                                .relationships(new Relationships.Builder()
                                                        .relationship("sources", new Relationship.Builder().build())
                                                        .relationship("consumers", new Relationship.Builder().build()))
                                                .attributes(groupTemplate.resourceObject().getAttributes())
                                                .build()));

                                Group updatedGroup = partialGroup.addSource(service);

                                if (updatedGroup.isComplete()) {
                                    partialGroups.remove(groupTemplate.resourceObject().getId());

                                    // New group based on template has been created
                                    LogManager.getLogger(getClass()).info("Match:" + updatedGroup);

                                    matchedTemplates.add(groupTemplate.resourceObject().getId());
                                    groups.addOrUpdateGroup(updatedGroup);
                                } else {
                                    LogManager.getLogger(getClass()).info("Partial match:" + updatedGroup);
                                    partialGroups.put(groupTemplate.resourceObject().getId(), updatedGroup);
                                }
                            });
                }

                if (groupTemplate.getConsumers().isConsumer(service)) {
                    // Check if group exists already
                    groups.getGroupByTemplate(groupTemplate)
                            .ifPresentOrElse(existingGroup ->
                            {
                                // Check if many cardinality
                                if (groupTemplate.getConsumers().isMany()) {
                                    groups.addOrUpdateGroup(existingGroup.addConsumer(service));
                                }
                            }, () ->
                            {
                                // Create partial group
                                Group partialGroup = partialGroups.computeIfAbsent(groupTemplate.resourceObject().getId(), templateId ->
                                        new Group(new ResourceObject.Builder("group", templateId)
                                                .relationships(new Relationships.Builder()
                                                        .relationship("sources", new Relationship.Builder().build())
                                                        .relationship("consumers", new Relationship.Builder().build()))
                                                .attributes(groupTemplate.resourceObject().getAttributes())
                                                .build()));

                                Group updatedGroup = partialGroup.addConsumer(service);

                                if (updatedGroup.isComplete()) {
                                    partialGroups.remove(groupTemplate.resourceObject().getId());

                                    // New group based on template has been created
                                    LogManager.getLogger(getClass()).info("Match:" + updatedGroup);

                                    matchedTemplates.add(groupTemplate.resourceObject().getId());
                                    groups.addOrUpdateGroup(updatedGroup);
                                } else {
                                    LogManager.getLogger(getClass()).info("Partial match:" + updatedGroup);
                                    partialGroups.put(groupTemplate.resourceObject().getId(), updatedGroup);
                                }
                            });
                }
            }
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not handle service", e);
        }
    }
}
