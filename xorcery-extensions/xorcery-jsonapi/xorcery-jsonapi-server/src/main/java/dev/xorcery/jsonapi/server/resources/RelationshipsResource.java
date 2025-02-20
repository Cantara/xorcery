/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.jsonapi.server.resources;

import dev.xorcery.jaxrs.server.resources.ContextResource;
import dev.xorcery.jsonapi.Links;
import dev.xorcery.jsonapi.Relationship;
import dev.xorcery.jsonapi.ResourceObject;
import dev.xorcery.jsonapi.ResourceObjects;
import jakarta.ws.rs.core.UriBuilder;

import java.net.URI;

public interface RelationshipsResource
        extends ContextResource {

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
