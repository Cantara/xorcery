package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
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
            for (GroupTemplate groupTemplate : groupTemplates) {
                groupTemplate.match(service).ifPresent(roi ->
                {
                    // Check if group exists already
                    groups.groupByTemplate(groupTemplate)
                            .ifPresentOrElse(existingGroup ->
                            {
                                // Check if many cardinality
                                if (groupTemplate.isMany(service)) {
                                    // Check if we have included data
                                    groupTemplate.resourceDocument().getIncluded().findByResourceObjectIdentifier(roi).ifPresentOrElse(ro ->
                                    {
                                        groups.addService(existingGroup, service, ro);
                                    }, () ->
                                    {
                                        groups.addService(existingGroup, service);
                                    });
                                }
                            }, () ->
                            {
                                // Create partial group
                                Group partialGroup = partialGroups.computeIfAbsent(groupTemplate.template().getId(), templateId ->
                                        new Group(new ResourceDocument.Builder()
                                                .data(new ResourceObject.Builder("group", templateId).build())
                                                .build()));

                                // Check if we have included data
                                Group updatedGroup = groupTemplate.resourceDocument().getIncluded().findByResourceObjectIdentifier(roi).map(ro ->
                                {
                                    return partialGroup.add(service, ro);
                                }).orElseGet(() ->
                                {
                                    return partialGroup.add(service);
                                });

                                if (groupTemplate.isMatched(updatedGroup)) {
                                    partialGroups.remove(groupTemplate.template().getId());

                                    // New group based on template has been created
                                    LogManager.getLogger(getClass()).info("Match:" + updatedGroup);

                                    matchedTemplates.add(groupTemplate.template().getId());
                                    groups.addGroup(updatedGroup);
                                } else {
                                    LogManager.getLogger(getClass()).info("Partial match:" + updatedGroup);
                                    partialGroups.put(groupTemplate.template().getId(), updatedGroup);
                                }
                            });
                });
            }
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not handle service", e);
        }
    }
}
