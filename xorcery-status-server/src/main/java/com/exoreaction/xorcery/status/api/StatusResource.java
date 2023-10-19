package com.exoreaction.xorcery.status.api;

import com.exoreaction.xorcery.hyperschema.Link;
import com.exoreaction.xorcery.jsonapi.*;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.jsonapischema.ResourceDocumentSchema;
import com.exoreaction.xorcery.jsonschema.JsonSchema;
import com.exoreaction.xorcery.jsonschema.server.resources.JsonSchemaMixin;
import com.exoreaction.xorcery.status.StatusProviders;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.xorcery.jsonapi.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.JsonApiRels.self;

@Path("api/status")
public class StatusResource
        extends JsonApiResource
        implements JsonSchemaMixin {

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
                .links(new com.exoreaction.xorcery.hyperschema.Links.Builder()
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
