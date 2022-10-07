package com.exoreaction.xorcery.service.eventstore.resources.api;

import com.exoreaction.xorcery.hyperschema.model.Link;
import com.exoreaction.xorcery.jsonapi.jaxrs.MediaTypes;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.jsonapischema.model.ResourceDocumentSchema;
import com.exoreaction.xorcery.jsonapischema.model.ResourceObjectSchema;
import com.exoreaction.xorcery.jsonschema.model.JsonSchema;
import com.exoreaction.xorcery.service.eventstore.model.EventStoreModel;
import com.exoreaction.xorcery.neo4j.jsonapi.resources.JsonSchemaMixin;
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
        return new ResourceDocumentSchema.Builder()
                .resources(streamSchema())
                .builder()
                .links(new com.exoreaction.xorcery.hyperschema.model.Links.Builder()
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
                        .link(describedby, getAbsolutePathBuilder().path(".schema"))
                        .link("stream", getUriBuilderFor(StreamResource.class).toTemplate())
                        .link("events", getBaseUriBuilder()
                                .scheme(getBaseUri().getScheme().equals("https") ? "wss" : "ws")
                                .path("ws/events"))
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
