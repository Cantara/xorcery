package com.exoreaction.xorcery.server.resources.api;

import com.exoreaction.xorcery.jsonapi.model.JsonApiRels;
import com.exoreaction.xorcery.jsonapi.model.Links;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResource;
import com.exoreaction.xorcery.server.Xorcery;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.registry.api.Registry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.glassfish.hk2.api.IterableProvider;

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
