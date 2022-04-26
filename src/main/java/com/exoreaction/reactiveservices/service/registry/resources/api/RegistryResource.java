package com.exoreaction.reactiveservices.service.registry.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.service.registry.resources.RegistryService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.JSON_API_TEXT_HTML;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path( "api/registry" )
public class RegistryResource
    extends JsonApiResource
{
    private final RegistryService service;

    @Inject
    public RegistryResource( RegistryService service )
    {
        this.service = service;
    }

    @GET
    @Produces(JSON_API_TEXT_HTML)
    public ResourceDocument registry()
    {
        return new ResourceDocument.Builder()
            .links( new Links.Builder()
                .link( "servers", getUriBuilderForPathFrom( RegistryServersResource.class ) )
                .link( "events", getBaseUriBuilder().scheme( "ws" ).path( "ws/registryevents" ) )
                .build())
            .build();
    }
}
