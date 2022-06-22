package com.exoreaction.xorcery.service.eventstore.resources.api;

import com.exoreaction.xorcery.hyperschema.model.HyperSchema;
import com.exoreaction.xorcery.jaxrs.MediaTypes;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResource;
import com.exoreaction.xorcery.jsonapi.resources.JsonSchemaMixin;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.xorcery.jsonapi.model.JsonApiRels.describedby;
import static com.exoreaction.xorcery.jsonapi.model.JsonApiRels.self;

@Path("api/eventstore")
public class EventStoreResource
        extends JsonApiResource
        implements JsonSchemaMixin {

    @Inject
    public EventStoreResource() {
    }

    @GET
    @Produces(MediaTypes.APPLICATION_JSON_SCHEMA)
    public JsonSchema schema() {
        HyperSchema.Builder builder = new HyperSchema.Builder(new JsonSchema.Builder());
        builder.links(new com.exoreaction.xorcery.hyperschema.model.Links.Builder()
                        .link(selfLink()).link(describedbyLink(getAbsolutePath().toASCIIString()))
                        .with(websocket("eventstorestreams", EventStoreParameters.class))
                        .build())
                .builder()
                .title("Forum application");
        return builder.build().schema();
    }

    @GET
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link(self, getUriInfo().getRequestUri().toASCIIString())
                        .link(describedby, getAbsolutePathBuilder().path(".schema"))
                        .link("eventstorestreams", getBaseUriBuilder()
                                .scheme(getBaseUri().getScheme().equals("https") ? "wss" : "ws")
                                .path("ws/eventstorestreams"))
                        .build())
                .build();
    }
}
