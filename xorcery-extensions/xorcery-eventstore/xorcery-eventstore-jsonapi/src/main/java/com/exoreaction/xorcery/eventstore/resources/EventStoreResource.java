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
package com.exoreaction.xorcery.eventstore.resources;

import com.exoreaction.xorcery.hyperschema.Link;
import com.exoreaction.xorcery.jaxrs.server.resources.BaseResource;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapischema.ResourceDocumentSchema;
import com.exoreaction.xorcery.jsonapischema.ResourceObjectSchema;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.server.resources.JsonSchemaResource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.Optional;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.self;

@Path("api/eventstore")
public class EventStoreResource
        extends BaseResource
        implements JsonSchemaResource {

    @Inject
    public EventStoreResource(Provider<EventStoreStreams> eventStoreService) {
        Optional.ofNullable(eventStoreService.get()).orElseThrow(NotFoundException::new);
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return new ResourceDocumentSchema.Builder()
                .resources(streamSchema())
                .builder()
                .links(new com.exoreaction.xorcery.hyperschema.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .with(websocket("events", EventStoreParameters.class),
                                l -> l.link(new Link.UriTemplateBuilder("stream")
                                        .parameter("id", "Stream id", "Stream id")
                                        .build()))
                        .build())
                .builder()
                .title("EventStore")
                .build();
    }

    @GET
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(self, getUriInfo().getRequestUri().toASCIIString())
                        .link(describedby, getAbsolutePathBuilder().path(".schema").toTemplate())
                        .link("stream", getUriBuilderFor(StreamResource.class).toTemplate())
                        .link("events", getBaseUriBuilder()
                                .scheme(getBaseUri().getScheme().equals("https") ? "wss" : "ws")
                                .path("ws/events")
                                .toTemplate())
                        .build())
                .build();
    }

    private ResourceObjectSchema streamSchema() {
        return new ResourceObjectSchema.Builder()
                .type(ApiTypes.stream)
                .attributes(attributes(EventStoreModel.Stream.values()))
                .with(b -> b.builder().builder().title("Stream"))
                .build();
    }

}
