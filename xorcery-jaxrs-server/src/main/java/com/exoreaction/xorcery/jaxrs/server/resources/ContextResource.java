package com.exoreaction.xorcery.jaxrs.server.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.jetty.ee10.servlet.ServletApiRequest;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Request;
import org.glassfish.hk2.api.ServiceLocator;

import javax.security.auth.Subject;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Optional;

/**
 * Helper methods for JAX-RS resources and resource interfaces to use. This is implemented by
 * AbstractResource which provides the injection and base objects used in the helpers. Other helper
 * interfaces can then extend this interface and use these methods without having access to AbstractResource.
 */
public interface ContextResource {

    // HK2
    ServiceLocator getServiceLocator();

    default <T> T service(Class<T> serviceType, Annotation... annotations) {
        T service = getServiceLocator().getService(serviceType, annotations);
        if (service == null)
            throw new IllegalArgumentException("No service of type found:" + serviceType);
        return service;
    }

    // Servlet
    HttpServletRequest getHttpServletRequest();
    HttpServletResponse getHttpServletResponse();

    // JAX-RS
    ContainerRequestContext getContainerRequestContext();

    default UriInfo getUriInfo() {
        return getContainerRequestContext().getUriInfo();
    }

    default String getFirstPathParameter(String parameterName) {
        return getUriInfo().getPathParameters().getFirst(parameterName);
    }

    default String getFirstQueryParameter(String parameterName) {
        return getUriInfo().getQueryParameters().getFirst(parameterName);
    }

    default UriBuilder getAbsolutePathBuilder() {
        return getUriInfo().getAbsolutePathBuilder();
    }

    default URI getBaseUri() {
        return getUriInfo().getBaseUri();
    }

    default UriBuilder getBaseUriBuilder() {
        return getUriInfo().getBaseUriBuilder();
    }

    default UriBuilder getRequestUriBuilder() {
        return getUriInfo().getRequestUriBuilder();
    }

    default URI getAbsolutePath() {
        return getUriInfo().getAbsolutePath();
    }

    default URI getParentPath() {
        String path = getAbsolutePath().getPath();
        path = path.substring(0, path.lastIndexOf('/'));
        return getAbsolutePathBuilder().replacePath(path).build();
    }

    default UriBuilder getUriBuilderFor(Class<?> resourceClass) {
        return getUriInfo().getBaseUriBuilder().path(resourceClass);
    }

    default SecurityContext getSecurityContext() {
        return getContainerRequestContext().getSecurityContext();
    }

    // JAAS
    default Optional<Subject> getSubject() {
        Optional<Subject> subject = Optional.empty();
        if (getHttpServletRequest() instanceof ServletApiRequest servletApiRequest)
        {
            AuthenticationState authenticationState = servletApiRequest.getAuthentication();
            if (authenticationState instanceof AuthenticationState.Deferred deferred)
            {
                AuthenticationState undeferred = deferred.authenticate(servletApiRequest.getRequest());
                if (undeferred != null && undeferred != authenticationState)
                {
                    authenticationState = undeferred;
                    AuthenticationState.setAuthenticationState(servletApiRequest.getRequest(), authenticationState);
                }
            }

            if (authenticationState instanceof LoginAuthenticator.UserAuthenticationSucceeded userAuth)
            {
                subject = Optional.of(userAuth.getUserIdentity().getSubject());
            }
        }
        return subject;
    }
}
