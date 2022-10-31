package com.exoreaction.xorcery.service.registry.resources.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.service.registry.RegistryState;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import static com.exoreaction.xorcery.jsonapi.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path("api/registry/servers")
public class RegistryServersResource {
    private final RegistryState registryState;

    @Inject
    public RegistryServersResource(RegistryState registryState) {
        this.registryState = registryState;
    }

    @GET
    @Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument servers() {
        return new ResourceDocument.Builder()
                .data(registryState.getServers().stream()
                        .flatMap(rd -> rd.resourceDocument().getResources().orElseThrow().stream())
                        .collect(ResourceObjects.toResourceObjects()))
                .build();
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
}
