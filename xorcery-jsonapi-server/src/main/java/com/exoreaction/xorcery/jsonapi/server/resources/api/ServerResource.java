package com.exoreaction.xorcery.jsonapi.server.resources.api;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.*;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path( "/" )
public class ServerResource
        extends JsonApiResource
{
    private ServiceResourceObjects serviceResourceObjects;
    private Configuration configuration;

    @Inject
    public ServerResource(ServiceResourceObjects serviceResourceObjects, Configuration configuration)
    {
        this.serviceResourceObjects = serviceResourceObjects;
        this.configuration = configuration;
    }

    @GET
    public ResourceDocument get()
    {
        StandardConfiguration standardConfiguration = ()->configuration;

        ResourceObjects.Builder builder = new ResourceObjects.Builder();
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {
            builder.resource(serviceResource.resourceObject());
        }
        ResourceDocument serverDocument = new ResourceDocument.Builder()
                .links(new Links.Builder().link(JsonApiRels.self, standardConfiguration.getServerUri()).build())
                .meta(new Meta.Builder()
                        .meta("timestamp", System.currentTimeMillis())
                        .build())
                .data(builder.build())
                .build();
        return serverDocument.resolve(getBaseUri());
    }
}