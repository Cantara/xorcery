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
package dev.xorcery.thymeleaf.resources;

import dev.xorcery.jaxrs.server.resources.ContextResource;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.core.Response;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.IServletWebApplication;
import org.thymeleaf.web.servlet.IServletWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Map;

public interface ThymeleafResource
    extends ContextResource
{
    default IServletWebApplication getWebApplication()
    {
            return JakartaServletWebApplication.buildApplication(getHttpServletRequest().getServletContext());
    }

    default IServletWebExchange getWebExchange()
    {
        return ((JakartaServletWebApplication)getWebApplication()).buildExchange(getHttpServletRequest(), getHttpServletResponse());
    }

    default WebContext newWebContext()
    {
        return new WebContext(getWebExchange(), getHttpServletRequest().getLocale());
    }

    default WebContext newWebContext(Map<String, Object> variables)
    {
        return new WebContext(getWebExchange(), getHttpServletRequest().getLocale(), variables);
    }

    default ITemplateEngine getTemplateEngine() {
        return getServiceLocator().getService(ITemplateEngine.class);
    }

    @OPTIONS
    default Response options() {
        return Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods", "POST, GET, PATCH, OPTIONS")
                .header("Access-Control-Allow-Headers", "content-type, accept, cookie, authorization")
                .build();
    }
}
