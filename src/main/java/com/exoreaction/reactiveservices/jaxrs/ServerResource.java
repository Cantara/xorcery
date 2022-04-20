package com.exoreaction.reactiveservices.jaxrs;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
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
    private ResourceDocument serverResourceDocument;

    @Inject
    public ServerResource( ResourceDocument serverResourceDocument )
    {
        this.serverResourceDocument = serverResourceDocument;
    }

    @GET
    @Produces(PRODUCES_JSON_API)
    public String get()
    {
        return serverResourceDocument.toString();
    }
}
