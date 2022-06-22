package com.exoreaction.xorcery.jsonapi.resources;

import com.exoreaction.xorcery.jsonapi.model.*;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;

public interface RelationshipsMixin
        extends ResourceContext {
    default Relationship relationship(ResourceObject resource) {
        return new Relationship.Builder().resourceIdentifier(resource).build();
    }

    default Relationship relationship(ResourceObjects resources, Links.Builder links) {
        return new Relationship.Builder()
                .resourceIdentifiers(resources)
                .with(b -> {
                    if (links != null && !links.builder().isEmpty()) b.links(links.build());
                })
                .build();
    }

    default URI relationshipRelatedLink(UriBuilder uriBuilder, Enum<?> relationshipName) {
        return uriBuilder.clone().path(relationshipName.name()).build();
    }
}
