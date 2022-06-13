package com.exoreaction.reactiveservices.jsonapi.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.hk2.api.ServiceLocator;

import static com.exoreaction.reactiveservices.jaxrs.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

@Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
public abstract class JsonApiResource
    implements ResourceContext
{
    @Inject
    private ServiceLocator serviceLocator;

    @Inject @Context
    private ContainerRequestContext containerRequestContext;

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