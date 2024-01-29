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
package com.exoreaction.xorcery.thymeleaf.resources;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.glassfish.hk2.api.ServiceLocator;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.web.servlet.IServletWebApplication;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Produces(TEXT_HTML)
public abstract class ThymeleafResource
    implements ResourceContext
{
    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private ITemplateEngine templateEngine;

    @Context
    private ContainerRequestContext containerRequestContext;

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    private JakartaServletWebApplication webApplication;

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public ContainerRequestContext getContainerRequestContext() {
        return containerRequestContext;
    }

    public HttpServletRequest getHttpServletRequest()
    {
        return httpServletRequest;
    }

    public HttpServletResponse getHttpServletResponse() {
        return httpServletResponse;
    }

    public IServletWebApplication getWebApplication()
    {
        if (webApplication == null)
        {
            webApplication = JakartaServletWebApplication.buildApplication(getHttpServletRequest().getServletContext());
        }
        return webApplication;
    }

    public IServletWebExchange getWebExchange()
    {
        return ((JakartaServletWebApplication)getWebApplication()).buildExchange(httpServletRequest, httpServletResponse);
    }

    public ITemplateEngine getTemplateEngine() {
        return templateEngine;
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
