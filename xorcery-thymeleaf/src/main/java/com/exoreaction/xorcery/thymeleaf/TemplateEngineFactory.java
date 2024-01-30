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
package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.WebApplicationTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

@Service
public class TemplateEngineFactory
        implements Factory<ITemplateEngine> {

    private final TemplateEngine templateEngine;

    @Inject
    public TemplateEngineFactory(ServletContextHandler servletContextHandler, Configuration configuration) {

/*
        final WebApplicationTemplateResolver templateResolver =
                new WebApplicationTemplateResolver(JakartaServletWebApplication.buildApplication(servletContextHandler.getServletContext()));
*/
        final ClassLoaderTemplateResolver templateResolver =
                new ClassLoaderTemplateResolver(ClassLoader.getSystemClassLoader());

        // HTML is the default mode, but we will set it anyway for better understanding of code
        templateResolver.setTemplateMode(TemplateMode.HTML);
        // This will convert "home" to "/WEB-INF/thymeleaf/templates/home.html"
        templateResolver.setPrefix("WEB-INF/thymeleaf/templates/");
        templateResolver.setSuffix(".html");

        // Cache is set to true by default. Set to false if you want templates to
        // be automatically updated when modified.
        templateResolver.setCacheable(false);
        // Set template cache TTL to 1 hour. If not set, entries would live in cache until expelled by LRU
        templateResolver.setCacheTTLMs(3600000L);

        templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
    }

    @Override
    @Singleton
    public ITemplateEngine provide()  {
        return templateEngine;
    }

    @Override
    public void dispose(ITemplateEngine instance) {

    }
}
