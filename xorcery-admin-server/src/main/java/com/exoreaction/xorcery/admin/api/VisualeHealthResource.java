package com.exoreaction.xorcery.admin.api;

import com.exoreaction.xorcery.health.registry.DefaultHealthCheckService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;

@Path("/health")
public class VisualeHealthResource {

    private static final CacheControl cacheControl = RuntimeDelegate.getInstance()
            .createHeaderDelegate(CacheControl.class)
            .fromString("must-revalidate,no-cache,no-store");

    private final DefaultHealthCheckService healthCheckService;

    @Inject
    public VisualeHealthResource(DefaultHealthCheckService defaultHealthCheckService) {
        this.healthCheckService = defaultHealthCheckService;
    }

    @GET
    public Response get(@QueryParam("compact") String compact) {
        boolean prettyPrint = !Boolean.parseBoolean(compact);
        StreamingOutput stream = out -> healthCheckService.writeHealthState(out, prettyPrint);
        return Response.ok(stream)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .cacheControl(cacheControl)
                .build();
    }
}
