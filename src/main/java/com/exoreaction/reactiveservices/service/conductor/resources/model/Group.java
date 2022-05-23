package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.jsonapi.model.*;
import com.exoreaction.reactiveservices.service.model.ServiceAttributes;
import com.exoreaction.reactiveservices.service.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static com.exoreaction.reactiveservices.jsonapi.model.Relationship.Builder.relationship;

public record Group(ResourceDocument resourceDocument) {
    public boolean isTemplate(GroupTemplate groupTemplate) {
        return group().getId().equals(groupTemplate.template().getId());
    }

    public Group add(Service service, ResourceObject settings) {
        ResourceObjectIdentifiers.Builder rois = new ResourceObjectIdentifiers.Builder();
        group().getRelationships().getRelationship("services")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .ifPresent(rois::resources);

        rois.resource(settings);

        Relationships.Builder builder = new Relationships.Builder();
        builder.relationship("services", new Relationship.Builder().resourceIdentifiers(rois.build()));

        Included.Builder included = new Included.Builder().with(resourceDocument().getIncluded());

        ResourceObject newSettings = new ResourceObject.Builder(settings.getType(), settings.getId())
                .attributes(settings.getAttributes())
                .relationships(new Relationships.Builder().relationship("service", relationship(service.resourceObject())))
                .build();
        included.add(newSettings);
        included.add(service.resourceObject());

        Group newGroup = new Group(new ResourceDocument.Builder()
                .data(new ResourceObject.Builder(group().getType(), group().getId())
                        .relationships(builder.build())
                        .build())
                .included(included.build())
                .build());
        return newGroup;
    }

    public Group add(Service service) {
        ResourceObjectIdentifiers.Builder rois = new ResourceObjectIdentifiers.Builder();
        group().getRelationships().getRelationship("services")
                .flatMap(Relationship::getResourceObjectIdentifiers)
                .ifPresent(rois::resources);

        rois.resource(service.resourceObject());

        Relationships.Builder builder = new Relationships.Builder();
        builder.relationship("services", new Relationship.Builder().resourceIdentifiers(rois.build()));

        Included.Builder included = new Included.Builder().with(resourceDocument().getIncluded());
        included.add(service.resourceObject());

        Group newGroup = new Group(new ResourceDocument.Builder()
                .data(new ResourceObject.Builder(group().getType(), group().getId())
                        .relationships(builder.build())
                        .build())
                .included(included.build())
                .build());
        return newGroup;
    }

    public ResourceObject group() {
        return resourceDocument().getResource().orElseThrow();
    }

    public List<Service> services() {
        return resourceDocument().getIncluded().getIncluded().stream()
                .map(Service::new)
                .collect(Collectors.toList());
    }

    public boolean contains(ServiceIdentifier serviceIdentifier) {
        return services().stream().anyMatch(service -> service.resourceObject().getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()));
    }

    public boolean contains(ServiceResourceObject serviceResourceObject) {
        return contains(serviceResourceObject.serviceIdentifier());
    }

    public void servicesByLinkRel(String rel, BiConsumer<ServiceResourceObject, Optional<ServiceAttributes>> serviceAttributesConsumer) {
        resourceDocument
                .getIncluded()
                .getIncluded()
                .stream()
                .filter(ro -> ro.getLinks().getRel(rel).isPresent())
                .forEach(ro ->
                {
                    ServiceResourceObject sro = new ServiceResourceObject(ro);

                    // Find service attributes, if any
                    Optional<ServiceAttributes> attributes = resourceDocument.getIncluded().getIncluded().stream()
                            .filter(iro -> iro.getRelationships().getRelationship("service")
                                    .flatMap(Relationship::getResourceObjectIdentifier)
                                    .map(roi -> roi.equals(ro.getResourceObjectIdentifier()))
                                    .orElse(false))
                            .findFirst()
                            .map(ResourceObject::getAttributes)
                            .map(ServiceAttributes::new);

                    serviceAttributesConsumer.accept(sro, attributes);
                });
    }

    public Optional<ServiceAttributes> serviceAttributes(ServiceIdentifier serviceIdentifier) {
        return resourceDocument
                .getIncluded()
                .getIncluded()
                .stream()
                .filter(ro -> ro.getRelationships()
                        .getRelationship("service")
                        .map(r -> r.contains(serviceIdentifier.resourceObjectIdentifier()))
                        .orElse(false))
                .map(ro -> new ServiceAttributes(ro.getAttributes())).findFirst();
    }
}
