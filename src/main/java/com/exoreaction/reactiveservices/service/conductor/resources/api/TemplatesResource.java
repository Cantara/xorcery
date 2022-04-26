package com.exoreaction.reactiveservices.service.conductor.resources.api;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObjects;
import com.exoreaction.reactiveservices.service.conductor.resources.ConductorService;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Template;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import java.io.StringReader;

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
                          .map(Template::getJson)
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
