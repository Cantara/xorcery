package com.exoreaction.xorcery.jaxrs.server.resources;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Base class for JAX-RS resource implementations. Injects a number of context objects and makes them available
 * through the ContextResource interface. Since the injection is done using a method the constructor can inject other
 * application-specific dependencies.
 */
public class BaseResource
    implements ContextResource
{
    private ServiceLocator serviceLocator;
    private ContainerRequestContext containerRequestContext;
    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;

    @Inject
    private void bind(
            ServiceLocator serviceLocator,
            ContainerRequestContext containerRequestContext,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    )
    {
        this.serviceLocator = serviceLocator;
        this.containerRequestContext = containerRequestContext;
        this.httpServletRequest = httpServletRequest;
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    @Override
    public HttpServletRequest getHttpServletRequest() {
        return httpServletRequest;
    }

    @Override
    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    @Override
    public ContainerRequestContext getContainerRequestContext() {
        return containerRequestContext;
    }
}
