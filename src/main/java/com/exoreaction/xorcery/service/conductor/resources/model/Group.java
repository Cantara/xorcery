package com.exoreaction.xorcery.service.conductor.resources.model;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Relationship;
import com.exoreaction.xorcery.jsonapi.model.Relationships;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjectIdentifiers;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;
import java.util.stream.Collectors;

public record Group(ResourceObject resourceObject, List<ServiceResourceObject> serviceResourceObjects) {

    public Group(ResourceObject resourceObject) {
        this(resourceObject, new ArrayList<>());
    }

    public boolean isTemplate(GroupTemplate groupTemplate) {
        return resourceObject().getId().equals(groupTemplate.resourceObject().getId());
    }

    public Group addSource(ServiceResourceObject serviceResourceObject) {
        serviceResourceObjects.add(serviceResourceObject);
        ArrayNode builder = resourceObject.getRelationships()
                .getRelationship("sources")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .map(ResourceObjectIdentifiers::json)
                .orElseGet(JsonNodeFactory.instance::arrayNode);

        return new Group(new ResourceObject.Builder(resourceObject)
                .relationships(new Relationships.Builder(resourceObject.getRelationships())
                        .relationship("sources", new Relationship.Builder()
                                .resourceIdentifiers(new ResourceObjectIdentifiers.Builder(builder)
                                        .resource(serviceResourceObject.resourceObject())
                                        .build()))).build(), serviceResourceObjects);
    }

    public Group addConsumer(ServiceResourceObject serviceResourceObject) {
        serviceResourceObjects.add(serviceResourceObject);
        ArrayNode builder = resourceObject.getRelationships()
                .getRelationship("consumers")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .map(ResourceObjectIdentifiers::json)
                .orElseGet(JsonNodeFactory.instance::arrayNode);

        return new Group(new ResourceObject.Builder(resourceObject)
                .relationships(new Relationships.Builder(resourceObject.getRelationships())
                        .relationship("consumers", new Relationship.Builder()
                                .resourceIdentifiers(new ResourceObjectIdentifiers.Builder(builder)
                                        .resource(serviceResourceObject.resourceObject())
                                        .build()))).build(), serviceResourceObjects);
    }

    public List<ServiceIdentifier> getSources() {
        return resourceObject().getRelationships()
                .getRelationship("sources")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .map(ResourceObjectIdentifiers::getResources)
                .map(roi -> roi.stream().map(ServiceIdentifier::new).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    public List<ServiceIdentifier> getConsumers() {
        return resourceObject().getRelationships()
                .getRelationship("consumers")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .map(ResourceObjectIdentifiers::getResources)
                .map(roi -> roi.stream().map(ServiceIdentifier::new).collect(Collectors.toList()))
                .orElseGet(Collections::emptyList);
    }

    public ServiceResourceObject getServiceResourceObject(ServiceIdentifier serviceIdentifier)
    {
        return serviceResourceObjects.stream().filter(sro -> sro.serviceIdentifier().equals(serviceIdentifier)).findFirst().orElseThrow(()->
        {
            return new IllegalStateException();
        });
    }

    public Configuration getSourceConfiguration() {
        return new Configuration(resourceObject().getAttributes().getAttribute("sources")
                .flatMap(json -> Optional.ofNullable((ObjectNode) json.get("configuration"))).orElseGet(JsonNodeFactory.instance::objectNode));
    }

    public Configuration getConsumerConfiguration() {
        return new Configuration(resourceObject().getAttributes().getAttribute("consumers")
                .flatMap(json -> Optional.ofNullable((ObjectNode) json.get("configuration"))).orElseGet(JsonNodeFactory.instance::objectNode));
    }

    public boolean isComplete() {
        return !(getSources().isEmpty() || getConsumers().isEmpty());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Group group = (Group) o;
        return resourceObject.equals(group.resourceObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceObject);
    }

}
