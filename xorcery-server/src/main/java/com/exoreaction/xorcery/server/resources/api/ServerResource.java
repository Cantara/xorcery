package com.exoreaction.xorcery.server.resources.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.service.registry.api.Registry;
import com.exoreaction.xorcery.service.registry.jsonapi.resources.JsonApiResource;
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
    private Registry registry;

    @Inject
    public ServerResource(Registry registry)
    {
        this.registry = registry;
    }

    @GET
    public ResourceDocument get()
    {
        return registry.getServer().resourceDocument().resolve(getBaseUri());
    }
}
