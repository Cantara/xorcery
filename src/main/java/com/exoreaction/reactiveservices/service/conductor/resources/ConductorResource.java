package com.exoreaction.reactiveservices.service.conductor.resources;

import com.exoreaction.reactiveservices.jaxrs.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path( "api/conductor" )
public class ConductorResource
    extends JsonApiResource
{
    private final ConductorService service;

    @Inject
    public ConductorResource( ConductorService service )
    {
        this.service = service;
    }

    @GET
    @Produces( PRODUCES_JSON_API )
    public String get()
    {
        return new ResourceDocument.Builder()
            .links( new Links.Builder()
                .link( "patterns", getUriBuilderForPathFrom( ConductorPatternsResource.class ) )
                .link( "events", getBaseUriBuilder().scheme( "ws" ).path( "ws/conductorevents" ) )
                .build())
            .build().toString();
    }
}
