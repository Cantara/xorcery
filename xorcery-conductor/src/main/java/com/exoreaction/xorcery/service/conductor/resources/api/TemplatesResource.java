package com.exoreaction.xorcery.service.conductor.resources.api;

import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.service.conductor.ConductorService;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@Path( "api/conductor/templates" )
public class TemplatesResource
{
    private ConductorService conductor;

    @Inject
    public TemplatesResource(ConductorService conductor )
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
