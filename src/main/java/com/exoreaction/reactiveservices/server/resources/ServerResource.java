package com.exoreaction.reactiveservices.server.resources;

import com.exoreaction.reactiveservices.jaxrs.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.server.Server;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

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
    @Produces(PRODUCES_JSON_API)
    public String get()
    {
        return server.getServerDocument().toString();
    }
}
