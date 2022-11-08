package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.Relationship;
import com.exoreaction.xorcery.jsonapi.model.Relationships;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Group;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import com.exoreaction.xorcery.service.conductor.api.ServiceTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupTemplates {

    private final Logger logger = LogManager.getLogger(getClass());

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
            logger.info("Check group templates for service: " + service.getServiceIdentifier());

            // Check if any group templates match
            // TODO This algo can be simplified
            for (GroupTemplate groupTemplate : groupTemplates) {
                if (matches(groupTemplate.getSources(), service)) {
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
                                    logger.info("Match:" + updatedGroup);

                                    matchedTemplates.add(groupTemplate.resourceObject().getId());
                                    groups.addOrUpdateGroup(updatedGroup);
                                } else {
                                    logger.debug("Partial match:" + updatedGroup);
                                    partialGroups.put(groupTemplate.resourceObject().getId(), updatedGroup);
                                }
                            });
                }

                if (matches(groupTemplate.getConsumers(),service)) {
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
                                    logger.info("Match:" + updatedGroup);

                                    matchedTemplates.add(groupTemplate.resourceObject().getId());
                                    groups.addOrUpdateGroup(updatedGroup);
                                } else {
                                    logger.debug("Partial match:" + updatedGroup);
                                    partialGroups.put(groupTemplate.resourceObject().getId(), updatedGroup);
                                }
                            });
                }
            }
        } catch (Exception e) {
            logger.error("Could not handle service", e);
        }
    }

    public boolean matches(ServiceTemplate template, ServiceResourceObject service) {
        String expression = template.pattern();
        List<Link> links = service.resourceObject().getLinks().getLinks();
        if (links.isEmpty()) {
            return new GroupTemplatePatternEvaluator(service, "").eval(expression);
        } else {
            return links.stream().anyMatch(link ->
                    new GroupTemplatePatternEvaluator(service, link.rel()).eval(expression));
        }
    }

}
