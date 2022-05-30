package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.Relationship;
import com.exoreaction.reactiveservices.jsonapi.model.Relationships;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjectIdentifiers;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record Group(ResourceObject resourceObject) {
    public boolean isTemplate(GroupTemplate groupTemplate) {
        return resourceObject().getId().equals(groupTemplate.resourceObject().getId());
    }

    public Group addSource(ServiceResourceObject serviceResourceObject) {
        JsonArrayBuilder builder = resourceObject.getRelationships()
                .getRelationship("sources")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .map(rois -> Json.createArrayBuilder(rois.json()))
                .orElseGet(Json::createArrayBuilder);

        return new Group(new ResourceObject.Builder(resourceObject)
                .relationships(new Relationships.Builder(resourceObject.getRelationships())
                        .relationship("sources", new Relationship.Builder()
                                .resourceIdentifiers(new ResourceObjectIdentifiers.Builder(builder)
                                        .resource(serviceResourceObject.resourceObject())
                                        .build()))).build());
    }

    public Group addConsumer(ServiceResourceObject serviceResourceObject) {
        JsonArrayBuilder builder = resourceObject.getRelationships()
                .getRelationship("consumers")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .map(rois -> Json.createArrayBuilder(rois.json()))
                .orElseGet(Json::createArrayBuilder);

        return new Group(new ResourceObject.Builder(resourceObject)
                .relationships(new Relationships.Builder(resourceObject.getRelationships())
                        .relationship("consumers", new Relationship.Builder()
                                .resourceIdentifiers(new ResourceObjectIdentifiers.Builder(builder)
                                        .resource(serviceResourceObject.resourceObject())
                                        .build()))).build());
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

    public Optional<JsonObject> getSourceParameters() {
        return resourceObject().getAttributes().getAttribute("sources").flatMap(json -> Optional.ofNullable(json.asJsonObject().getJsonObject("parameters")));
    }

    public Optional<JsonObject> getConsumerParameters() {
        return resourceObject().getAttributes().getAttribute("consumers").flatMap(json -> Optional.ofNullable(json.asJsonObject().getJsonObject("parameters")));
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
