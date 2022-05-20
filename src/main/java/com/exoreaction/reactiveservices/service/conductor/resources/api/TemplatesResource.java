package com.exoreaction.reactiveservices.service.conductor.resources.api;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.service.conductor.ConductorService;
import com.exoreaction.reactiveservices.service.conductor.resources.model.GroupTemplate;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path( "api/conductor/templates" )
public class TemplatesResource
{
    private final ConductorService service;

    @Inject
    public TemplatesResource(ConductorService service )
    {
        this.service = service;
    }

    @GET
    @Produces( MediaTypes.JSON_API_TEXT_HTML )
    public ResourceDocument get()
    {
        return new ResourceDocument.Builder()
            .data( service.getTemplates().stream()
                          .map(GroupTemplate::template)
                          .collect( ResourceObjects.toResourceObjects() ) )
            .build();
    }

    @POST
    @Consumes( MediaTypes.APPLICATION_JSON_API )
    public void add( String resourceObject )
    {
//        service.addTemplates( new ResourceDocument( Json.createReader( new StringReader( resourceObject ) ).read() ) );
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
    @Path( "{id}" )
    public void remove( @PathParam( "id" ) String id )
    {
        service.removeTemplate( id );
    }
}
