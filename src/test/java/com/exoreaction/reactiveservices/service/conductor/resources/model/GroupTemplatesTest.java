package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import jakarta.json.Json;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class GroupTemplatesTest {

    Logger logger = LogManager.getLogger(getClass());

    @Test
    public void givenTemplateWhenServicesProvidedThenCreateGroup() {
        // Given
        AtomicInteger count = new AtomicInteger();
        Groups groups = new Groups(new Groups.GroupsListener() {
            @Override
            public void addedGroup(Group group) {
                logger.info("Created group " + group);
                count.incrementAndGet();
            }
        });

        Configuration configuration = new Configuration.Builder()
                .add("environment", "development")
                .build();

        GroupTemplates groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {
            }
        }, groups, configuration);

        new ResourceDocument(Json.createReader(getClass().getResourceAsStream("/grouptemplates.json")).readObject())
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

        assertThat(count.get(), equalTo(1));
    }

    @Test
    public void givenTemplateAndGroupWhenServiceProvidedThenExpandGroup() {
        // Given
        AtomicInteger count = new AtomicInteger();
        Configuration configuration = new Configuration.Builder()
                .add("environment", "development")
                .build();
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
        }, groups, configuration);

        new ResourceDocument(Json.createReader(getClass().getResourceAsStream("/grouptemplates.json")).readObject())
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
        assertThat(count.get(), equalTo(1));
    }
}