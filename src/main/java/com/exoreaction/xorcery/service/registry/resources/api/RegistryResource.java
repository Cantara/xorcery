package com.exoreaction.xorcery.service.registry.resources.api;

import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResource;
import com.exoreaction.xorcery.service.registry.api.Registry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.xorcery.jaxrs.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path( "api/registry" )
public class RegistryResource
    extends JsonApiResource
{
    private final Registry service;

    @Inject
    public RegistryResource( Registry service )
    {
        this.service = service;
    }

    @GET
    @Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument registry()
    {
        return new ResourceDocument.Builder()
            .links( new Links.Builder()
                .link( "servers", getUriBuilderFor( RegistryServersResource.class ) )
                .link( "events", getBaseUriBuilder().scheme( "ws" ).path( "ws/registryevents" ) )
                .build())
            .build();
    }
}
