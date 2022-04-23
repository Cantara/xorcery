package com.exoreaction.reactiveservices.jaxrs.resources;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import jakarta.inject.Inject;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.hk2.api.ServiceLocator;

public abstract class JsonApiResource
    implements ResourceContext
{
    protected static final String PRODUCES_JSON_API = ResourceDocument.APPLICATION_JSON_API + ";qs=1.0";
 //   protected static final String PRODUCES_JSON_SCHEMA = JsonSchema.JSON_SCHEMA + ";qs=0.1";

    private ServiceLocator serviceLocator;
    private ContainerRequestContext containerRequestContext;

    @Inject
    public void request( @Context ContainerRequestContext containerRequestContext, ServiceLocator serviceLocator )
    {
        this.containerRequestContext = containerRequestContext;
        this.serviceLocator = serviceLocator;
    }

    public ServiceLocator getServiceLocator()
    {
        return serviceLocator;
    }

    public SecurityContext getSecurityContext()
    {
        return containerRequestContext.getSecurityContext();
    }

    public UriInfo getUriInfo()
    {
        return containerRequestContext.getUriInfo();
    }

    public ContainerRequestContext getContainerRequestContext()
    {
        return containerRequestContext;
    }

    @OPTIONS
    public Response options()
    {
        return Response.ok()
                       .header( "Access-Control-Allow-Origin", "*" )
                       .header( "Access-Control-Allow-Methods", "POST, GET, PATCH, OPTIONS" )
                       .header( "Access-Control-Allow-Headers", "content-type, accept, cookie, authorization" )
                       .build();
    }
}