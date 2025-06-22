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
package dev.xorcery.kurrent.jsonapi.resources;

import dev.xorcery.hyperschema.Link;
import dev.xorcery.jaxrs.server.resources.BaseResource;
import dev.xorcery.jsonapi.Links;
import dev.xorcery.jsonapi.MediaTypes;
import dev.xorcery.jsonapi.ResourceDocument;
import dev.xorcery.jsonapischema.ResourceDocumentSchema;
import dev.xorcery.jsonapischema.ResourceObjectSchema;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.server.resources.JsonSchemaResource;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import java.util.Optional;

import static dev.xorcery.jsonapi.JsonApiRels.describedby;
import static dev.xorcery.jsonapi.JsonApiRels.self;

@Path("api/kurrent")
public class KurrentResource
        extends BaseResource
        implements JsonSchemaResource {

    @Inject
    public KurrentResource(Provider<KurrentStreams> kurrentStreamsProvider) {
        Optional.ofNullable(kurrentStreamsProvider.get()).orElseThrow(NotFoundException::new);
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return new ResourceDocumentSchema.Builder()
                .resources(streamSchema())
                .builder()
                .links(new dev.xorcery.hyperschema.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .with(websocket("events", KurrentParameters.class),
                                l -> l.link(new Link.UriTemplateBuilder("streamId")
                                        .parameter("id", "Stream id", "Stream id")
                                        .build()))
                        .build())
                .builder()
                .title("Kurrent")
                .build();
    }

    @GET
    public ResourceDocument get() {
        return ResourceDocument.newResourceDocument()
                .links(new Links.Builder()
                        .link(self, getUriInfo().getRequestUri().toASCIIString())
                        .link(describedby, getAbsolutePathBuilder().path(".schema").toTemplate())
                        .link("streamId", getUriBuilderFor(StreamResource.class).toTemplate())
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
                .attributes(attributes(KurrentModel.Stream.values()))
                .with(b -> b.builder().builder().title("Stream"))
                .build();
    }
}
