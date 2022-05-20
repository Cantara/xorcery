package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import jakarta.json.Json;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

class GroupTemplatesTest {

    @Test
    public void givenTemplateWhenServicesProvidedThenCreateGroup() {
        // Given
        AtomicInteger count = new AtomicInteger();
        Groups groups = new Groups(new Groups.GroupsListener() {
            @Override
            public void addedGroup(Group group) {
                System.out.println("Created group "+group);
                count.incrementAndGet();
            }
        });

        GroupTemplates groupTemplates = new GroupTemplates(new GroupTemplates.GroupTemplatesListener() {
            @Override
            public void addedTemplate(GroupTemplate groupTemplate) {

            }
        }, groups);

        GroupTemplate template = new GroupTemplate(new ResourceDocument(Json.createReader(getClass().getResourceAsStream("/grouptemplates.json")).readObject()));

        groupTemplates.addTemplate(template);

        // When
        groupTemplates.addedService(new Service(new ResourceObject.Builder("logappender", "server1")
                .links(new Links.Builder().link("logevents", "ws:///ws/logevents"))
                .build()));

        groupTemplates.addedService(new Service(new ResourceObject.Builder("sysout", "server1")
                .build()));

        // Then
        assertThat(count.get(), equalTo(1));
    }

}