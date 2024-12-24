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
package dev.xorcery.thymeleaf;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.hk2.Services;
import jakarta.inject.Inject;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Handler;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

@Service(name = "thymeleaf.webapplication")
public class WebApplicationTemplateResolverFactory
        extends TemplateResolverFactory {
    @Inject
    public WebApplicationTemplateResolverFactory(Configuration configuration, IterableProvider<Handler> handlers) {
        super(
                new TemplateResolverConfiguration(configuration.getConfiguration("thymeleaf.webapplication")),
                new WebApplicationTemplateResolver(JakartaServletWebApplication.buildApplication(Services.ofType(handlers, ServletContextHandler.class).get().getServletContext()))
        );
    }

    @Override
    public ITemplateResolver provide() {
        return super.provide();
    }
}
