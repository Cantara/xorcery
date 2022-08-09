package com.exoreaction.xorcery.server.resources.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResource;
import com.exoreaction.xorcery.server.Xorcery;
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
    private Xorcery xorcery;

    @Inject
    public ServerResource( Xorcery xorcery)
    {
        this.xorcery = xorcery;
    }

    @GET
    public ResourceDocument get()
    {
        return xorcery.getServerDocument().resolve(getBaseUri());
    }
}
