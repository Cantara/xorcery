package com.exoreaction.reactiveservices.service.conductor.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.service.conductor.resources.ConductorService;
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
    public ResourceDocument get()
    {
        return new ResourceDocument.Builder()
            .links( new Links.Builder()
                .link( "templates", getUriBuilderForPathFrom( TemplatesResource.class ) )
                .link( "conductorevents", getBaseUriBuilder().scheme( "ws" ).path( "ws/conductorevents" ) )
                .build())
            .build();
    }
}
