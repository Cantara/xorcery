package com.exoreaction.xorcery.service.conductor.resources.api;

import com.exoreaction.xorcery.jaxrs.MediaTypes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.conductor.resources.model.Group;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path("api/conductor/groups")
public class GroupsResource {
    private Conductor conductor;

    @Inject
    public GroupsResource(Conductor conductor) {
        this.conductor = conductor;
    }

    @GET
    @Produces(MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument get() {
        return new ResourceDocument.Builder()
                .data(conductor.getGroups().getGroups().stream()
                        .map(Group::resourceObject)
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
