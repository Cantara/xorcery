package com.exoreaction.xorcery.service.conductor.test.resource.model;

import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.GroupTemplates;
import com.exoreaction.xorcery.service.conductor.Groups;
import com.exoreaction.xorcery.service.conductor.api.Group;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

class GroupTemplatesTest {

    Logger logger = LogManager.getLogger(getClass());

    @Test
    public void givenTemplateWhenServicesProvidedThenCreateGroup() throws IOException {
        // Given
        AtomicInteger count = new AtomicInteger();
        Groups groups = new Groups(new Groups.GroupsListener() {
            @Override
            public void addedGroup(Group group) {
                logger.info("Created group " + group);
                count.incrementAndGet();
            }
        });

        GroupTemplates groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {
            }
        }, groups);

        new ResourceDocument((ObjectNode)new ObjectMapper().readTree(getClass().getResource("/grouptemplates.json")))
                .getResources().map(ResourceObjects::getResources).orElseGet(Collections::emptyList).forEach(ro ->
                {
                    GroupTemplate template = new GroupTemplate(ro);
                    groupTemplates.addTemplate(template);
                });

        // When
        groupTemplates.addedService(new ServiceResourceObject(new ResourceObject.Builder("logappender", "server1")
                .links(new Links.Builder().link("logevents", "ws:///ws/logevents"))
                .build()));

        groupTemplates.addedService(new ServiceResourceObject(new ResourceObject.Builder("soutlogging", "server1")
                .build()));

        // Then
        System.out.println(groups.getGroups().get(0).resourceObject().toJsonString());

        MatcherAssert.assertThat(count.get(), CoreMatchers.equalTo(1));
    }

    @Test
    public void givenTemplateAndGroupWhenServiceProvidedThenExpandGroup() throws IOException {
        // Given
        AtomicInteger count = new AtomicInteger();
        Groups groups = new Groups(new Groups.GroupsListener() {
            @Override
            public void addedGroup(Group group) {
                logger.info("Created group " + group);
            }

            @Override
            public void updatedGroup(Group group) {
                logger.info("Updated group " + group);
                count.incrementAndGet();
            }
        });

        GroupTemplates groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {

            }
        }, groups);

        new ResourceDocument((ObjectNode)new ObjectMapper().readTree(getClass().getResource("/grouptemplates.json")))
                .getResources().map(ResourceObjects::getResources).orElseGet(Collections::emptyList).forEach(ro ->
                {
                    GroupTemplate template = new GroupTemplate(ro);
                    groupTemplates.addTemplate(template);
                });

        groupTemplates.addedService(new ServiceResourceObject(new ResourceObject.Builder("logappender", "server1")
                .links(new Links.Builder().link("logevents", "ws:///ws/logevents"))
                .build()));

        groupTemplates.addedService(new ServiceResourceObject(new ResourceObject.Builder("soutlogging", "server1")
                .build()));

        // When
        groupTemplates.addedService(new ServiceResourceObject(new ResourceObject.Builder("logappender", "server2")
                .links(new Links.Builder().link("logevents", "ws:///ws/logevents"))
                .build()));

        // Then
        MatcherAssert.assertThat(count.get(), CoreMatchers.equalTo(1));
    }
}