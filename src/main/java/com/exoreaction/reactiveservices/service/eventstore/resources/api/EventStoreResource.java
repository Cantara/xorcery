package com.exoreaction.reactiveservices.service.eventstore.resources.api;

import com.exoreaction.reactiveservices.hyperschema.model.HyperSchema;
import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.resources.JsonSchemaMixin;
import com.exoreaction.reactiveservices.jsonschema.model.JsonSchema;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.describedby;
import static com.exoreaction.reactiveservices.jsonapi.model.JsonApiRels.self;

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
        builder.links(new com.exoreaction.reactiveservices.hyperschema.model.Links.Builder()
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
