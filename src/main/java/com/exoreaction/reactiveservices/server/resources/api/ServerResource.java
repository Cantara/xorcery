package com.exoreaction.reactiveservices.server.resources.api;

import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.model.ResourceDocument;
import com.exoreaction.reactiveservices.server.Server;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

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
    public ResourceDocument get()
    {
        return server.getServerDocument();
    }
}
