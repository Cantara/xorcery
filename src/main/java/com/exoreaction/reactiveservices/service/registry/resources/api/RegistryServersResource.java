package com.exoreaction.reactiveservices.service.registry.resources.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path("api/registry/servers")
public class RegistryServersResource {
    private final Registry service;

    @Inject
    public RegistryServersResource(Registry service) {
        this.service = service;
    }

    @GET
    @Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument servers() {
        return new ResourceDocument.Builder()
                .data(service.getServers().stream()
                        .flatMap(rd -> rd.resourceDocument().getResources().orElseThrow().stream())
                        .collect(ResourceObjects.toResourceObjects()))
                .build();
    }

    @POST
    @Consumes(PRODUCES_JSON_API_TEXT_HTML_YAML)
    public void add(ResourceDocument resourceDocument) {
        service.addServer(new ServerResourceDocument(resourceDocument));
    }

/*
    @GET
    @Produces( ResourceDocument.APPLICATION_JSON_API )
    @Path( "{id}" )
    public String service( @PathParam( "id" ) String id )
    {
        return new ResourceDocument.Builder()
            .data( service.getServers().stream().filter( ro -> ro.getId().equals( id ) ).findFirst()
                          .orElseThrow( NotFoundException::new ) )
            .build().toString();
    }
*/

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        service.removeServer(id);
    }
}
