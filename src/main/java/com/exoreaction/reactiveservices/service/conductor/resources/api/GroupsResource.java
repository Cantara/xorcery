package com.exoreaction.reactiveservices.service.conductor.resources.api;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.service.conductor.resources.model.GroupTemplate;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;

import java.io.StringReader;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path( "api/conductor/groups" )
public class GroupsResource
{
    private Conductor conductor;

    @Inject
    public GroupsResource(Conductor conductor )
    {
        this.conductor = conductor;
    }

    @GET
    @Produces( MediaTypes.JSON_API_TEXT_HTML )
    public ResourceDocument get()
    {
        return new ResourceDocument.Builder()
            .data( conductor.getGroups().getGroups().stream()
                          .map(Group::group)
                          .collect( ResourceObjects.toResourceObjects() ) )
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
