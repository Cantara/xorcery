package com.exoreaction.reactiveservices.server.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.Server;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.JSON_API_TEXT_HTML;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

@Path( "/" )
public class ServerResource
    extends JsonApiResource
{
    private Server server;

    @Inject
    public ServerResource( Server server )
    {
        this.server = server;
    }

    @GET
    @Produces(JSON_API_TEXT_HTML)
    public ResourceDocument get()
    {
        return server.getServerDocument();
    }
}
