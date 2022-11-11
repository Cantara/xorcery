package com.exoreaction.xorcery.service.registry.resources.api;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.registry.RegistryState;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
    private Configuration configuration;

    @Inject
    public ServerResource(RegistryState registryState, Configuration configuration)
    {
        this.registryState = registryState;
        this.configuration = configuration;
    }

    @GET
    public ResourceDocument get()
    {
        ResourceDocument resourceDocument = registryState.getServer().resourceDocument().resolve(getBaseUri());
        resourceDocument.getMeta().json().set("port", JsonNodeFactory.instance.numberNode(configuration.getInteger("server.port").orElse(-1)));
        return resourceDocument;
    }
}
