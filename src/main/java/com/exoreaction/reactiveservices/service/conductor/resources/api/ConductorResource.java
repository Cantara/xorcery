package com.exoreaction.reactiveservices.service.conductor.resources.api;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
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
