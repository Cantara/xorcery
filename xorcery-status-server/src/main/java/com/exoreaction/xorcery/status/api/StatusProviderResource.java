package com.exoreaction.xorcery.status.api;

import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.status.StatusProviders;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import java.util.Optional;

@Path("api/status/{name}")
public class StatusProviderResource {

    private final StatusProviders statusProviders;

    @Inject
    public StatusProviderResource(StatusProviders statusProviders) {
        this.statusProviders = statusProviders;
    }

    @GET
    public ResourceDocument get(@PathParam("name") String name, @QueryParam("include") String include)
    {
        return statusProviders.getResourceDocument(name, Optional.ofNullable(include).orElse(""));
    }
}
