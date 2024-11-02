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
package dev.xorcery.status.api;

import dev.xorcery.hyperschema.Link;
import dev.xorcery.jaxrs.server.resources.BaseResource;
import dev.xorcery.jsonapi.*;
import dev.xorcery.jsonapi.server.resources.JsonApiResource;
import dev.xorcery.jsonapischema.ResourceDocumentSchema;
import dev.xorcery.jsonschema.JsonSchema;
import dev.xorcery.jsonschema.server.resources.JsonSchemaResource;
import dev.xorcery.status.StatusProviders;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static dev.xorcery.jsonapi.JsonApiRels.describedby;
import static dev.xorcery.jsonapi.JsonApiRels.self;

@Path("api/status")
public class StatusResource
        extends BaseResource
        implements JsonApiResource, JsonSchemaResource {

    private final StatusProviders statusProviders;

    @Inject
    public StatusResource(StatusProviders statusProviders) {
        this.statusProviders = statusProviders;
    }

    @GET
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(self, getUriInfo().getRequestUri().toASCIIString())
                        .link(describedby, getAbsolutePathBuilder().path(".schema").toTemplate()))
                .data(new ResourceObjects.Builder()
                        .with(b ->
                        {
                            for (String statusProviderName : statusProviders.getStatusProviderNames()) {
                                String statusTemplate = getAbsolutePathBuilder()
                                        .path(statusProviderName)
                                        .queryParam("include", "{include}")
                                        .toTemplate();
                                b.resource(new ResourceObject.Builder("status", statusProviderName)
                                        .links(new Links.Builder()
                                                .link("self", statusTemplate)
                                                .build())
                                        .build());
                            }
                        })
                )
                .build();
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        return new ResourceDocumentSchema.Builder()
                .builder()
                .links(new dev.xorcery.hyperschema.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .with(l -> l.link(new Link.UriTemplateBuilder("self")
                                        .parameter("include", "Include filter", "Filter of status items to include")
                                        .build()))
                        .build())
                .builder()
                .title("Status")
                .build();
    }
}
