package com.exoreaction.xorcery.service.registry.resources.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.service.registry.RegistryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import static com.exoreaction.xorcery.jsonapi.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path("api/registry/servers")
public class RegistryServersResource {
    private final RegistryService service;

    @Inject
    public RegistryServersResource(RegistryService service) {
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

/*
    @POST
    @Consumes(PRODUCES_JSON_API_TEXT_HTML_YAML)
    public void add(ResourceDocument resourceDocument) {
        service.addServer(new ServerResourceDocument(resourceDocument));
    }
*/

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
/*

    @DELETE
    @Path("{id}")
    public void remove(@PathParam("id") String id) {
        service.removeServer(id);
    }
*/
}
