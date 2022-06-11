package com.exoreaction.reactiveservices.service.conductor.resources.api;

import com.exoreaction.reactiveservices.jaxrs.MediaTypes;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceObjects;
import com.exoreaction.reactiveservices.service.conductor.api.Conductor;
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
    private Conductor conductor;

    @Inject
    public TemplatesResource(Conductor conductor )
    {
        this.conductor = conductor;
    }

    @GET
    @Produces( MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument get()
    {
        return new ResourceDocument.Builder()
            .data( conductor.getGroupTemplates().getTemplates().stream()
                          .map(GroupTemplate::resourceObject)
                          .collect( ResourceObjects.toResourceObjects() ) )
            .build();
    }

    @POST
    @Consumes( MediaTypes.APPLICATION_JSON_API )
    public void add( ResourceDocument resourceDocument )
    {
        conductor.getGroupTemplates().addTemplate(new GroupTemplate(resourceDocument.getResource().get()) );
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
//        conductor.getGroupTemplates().removeTemplate( id );
    }
}
