package com.exoreaction.reactiveservices.server;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * @author rickardoberg
 * @since 14/04/2022
 */

@Path("/")
public class ServerResource
{
    private final ResourceDocument serverDescriptor;

    @Inject
    public ServerResource( ResourceDocument serverDescriptor )
    {
        this.serverDescriptor = serverDescriptor;
    }

    @GET
    public String get()
    {
        return serverDescriptor.toString();
    }
}
