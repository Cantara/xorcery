package com.exoreaction.reactiveservices.jaxrs;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.hk2.api.ServiceLocator;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * @author rickardoberg
 * @since 27/04/2016
 */
public interface ResourceContext
{
    ContainerRequestContext getContainerRequestContext();

    ServiceLocator getServiceLocator();

    default <T> T service( Class<T> serviceType, Annotation... annotations )
    {
        T service = getServiceLocator().getService( serviceType, annotations );
        if (service == null)
            throw new IllegalArgumentException("No service of type found:"+serviceType);
        return service;
    }

    default SecurityContext getSecurityContext()
    {
        return getContainerRequestContext().getSecurityContext();
    }

    default UriInfo getUriInfo()
    {
        return getContainerRequestContext().getUriInfo();
    }

    default String getFirstPathParameter( String parameterName )
    {
        return getUriInfo().getPathParameters().getFirst( parameterName );
    }

    default String getFirstQueryParameter( String parameterName )
    {
        return getUriInfo().getQueryParameters().getFirst( parameterName );
    }

    default UriBuilder getAbsolutePathBuilder()
    {
        return getUriInfo().getAbsolutePathBuilder();
    }

    default UriBuilder getBaseUriBuilder()
    {
        return getUriInfo().getBaseUriBuilder();
    }

    default UriBuilder getRequestUriBuilder()
    {
        return getUriInfo().getRequestUriBuilder();
    }

    default URI getAbsolutePath()
    {
        return getUriInfo().getAbsolutePath();
    }

    default URI getParentPath()
    {
        String path = getAbsolutePath().getPath();
        path = path.substring( 0, path.lastIndexOf( '/' ) );
        return getAbsolutePathBuilder().replacePath( path ).build();
    }

    default UriBuilder getUriBuilderForPathFrom( Class<?> resourceClass )
    {
        return getUriInfo().getBaseUriBuilder().path( resourceClass );
    }

    default UriBuilder getUriBuilderForPathFrom( Method resourceMethod )
    {
        return getUriInfo().getBaseUriBuilder().path( resourceMethod );
    }

    default Cookie getUserCookie()
    {
        return getContainerRequestContext().getCookies().get( "token" );
    }
}
