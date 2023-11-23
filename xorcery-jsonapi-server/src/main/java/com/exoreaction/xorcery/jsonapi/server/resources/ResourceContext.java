/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.jsonapi.server.resources;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.glassfish.hk2.api.ServiceLocator;

import javax.security.auth.Subject;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Optional;

/**
 * @author rickardoberg
 */
public interface ResourceContext {
    ContainerRequestContext getContainerRequestContext();

    HttpServletRequest getHttpServletRequest();

    ServiceLocator getServiceLocator();

    default <T> T service(Class<T> serviceType, Annotation... annotations) {
        T service = getServiceLocator().getService(serviceType, annotations);
        if (service == null)
            throw new IllegalArgumentException("No service of type found:" + serviceType);
        return service;
    }

    default SecurityContext getSecurityContext() {
        return getContainerRequestContext().getSecurityContext();
    }

    default Subject getSubject() {
        return Optional.ofNullable(((Request) getHttpServletRequest()).getUserIdentity()).map(UserIdentity::getSubject).orElse(null);
    }

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

    default Cookie getUserCookie() {
        return getContainerRequestContext().getCookies().get("token");
    }


    default ObjectMapper objectMapper() {
        return objectMapper;
    }

    ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
}
