package com.exoreaction.xorcery.service.registry.resources.api;

import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.registry.RegistryState;
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
    private RegistryState registryState;

    @Inject
    public ServerResource(RegistryState registryState)
    {
        this.registryState = registryState;
    }

    @GET
    public ResourceDocument get()
    {
        return registryState.getServer().resourceDocument().resolve(getBaseUri());
    }
}
