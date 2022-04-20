package com.exoreaction.reactiveservices.service.conductor.resources;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObjects;
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

@Path( "api/conductor/patterns" )
public class ConductorPatternsResource
{
    private final ConductorService service;

    @Inject
    public ConductorPatternsResource( ConductorService service )
    {
        this.service = service;
    }

    @GET
    @Produces( ResourceDocument.APPLICATION_JSON_API )
    public String servers()
    {
        return new ResourceDocument.Builder()
            .data( service.getPatterns().stream()
                          .flatMap( rd -> rd.getResources().orElseThrow().getResources().stream() )
                          .collect( ResourceObjects.toResourceObjects() ) )
            .build().toString();
    }

    @POST
    @Consumes( ResourceDocument.APPLICATION_JSON_API )
    public void add( String resourceObject )
    {
        service.addPattern( new ResourceDocument( Json.createReader( new StringReader( resourceObject ) ).read() ) );
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
        service.removePattern( id );
    }
}
