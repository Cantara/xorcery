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

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.UserIdentity;
import org.glassfish.hk2.api.ServiceLocator;

import javax.security.auth.Subject;
import java.util.Optional;

import static com.exoreaction.xorcery.jsonapi.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;

@Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
public abstract class JsonApiResource
        implements ResourceContext {
    @Inject
    private ServiceLocator serviceLocator;

    @Context
    private ContainerRequestContext containerRequestContext;

    @Context
    private HttpServletRequest httpServletRequest;

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public SecurityContext getSecurityContext() {
        return containerRequestContext.getSecurityContext();
    }

    public UriInfo getUriInfo() {
        return containerRequestContext.getUriInfo();
    }

    public ContainerRequestContext getContainerRequestContext() {
        return containerRequestContext;
    }

    public HttpServletRequest getHttpServletRequest()
    {
        return httpServletRequest;
    }

    @OPTIONS
    public Response options() {
        return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, PATCH, OPTIONS")
                .header("Access-Control-Allow-Headers", "content-type, accept, cookie, authorization")
                .build();
    }
}