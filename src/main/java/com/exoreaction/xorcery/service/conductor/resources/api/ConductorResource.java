package com.exoreaction.xorcery.service.conductor.resources.api;

import com.exoreaction.xorcery.jaxrs.MediaTypes;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResource;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
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
    private final Conductor conductor;

    @Inject
    public ConductorResource( Conductor conductor )
    {
        this.conductor = conductor;
    }

    @GET
    @Produces( MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument get()
    {
        return new ResourceDocument.Builder()
            .links( new Links.Builder()
                .link( "templates", getUriBuilderFor( TemplatesResource.class ) )
                .link( "conductorevents", getBaseUriBuilder().scheme( "ws" ).path( "ws/conductorevents" ) )
                .build())
            .build();
    }
}
