package com.exoreaction.reactiveservices.service.registry.resources.api;

import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.server.model.ServerResourceDocument;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;

import java.io.StringReader;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.JSON_API_TEXT_HTML;

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
    @Produces(JSON_API_TEXT_HTML)
    public ResourceDocument servers() {
        return new ResourceDocument.Builder()
                .data(service.getServers().stream()
                        .flatMap(rd -> rd.resourceDocument().getResources().orElseThrow().getResources().stream())
                        .collect(ResourceObjects.toResourceObjects()))
                .build();
    }

    @POST
    @Consumes(JSON_API_TEXT_HTML)
    public void add(String resourceObject) {
        service.addServer(new ServerResourceDocument(new ResourceDocument(Json.createReader(new StringReader(resourceObject)).readObject())));
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
