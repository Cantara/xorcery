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
package com.exoreaction.xorcery.jetty.server;


import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
@RunLevel(4)
public class ServletContextHandlerFactory
        implements Factory<ServletContextHandler> {
    private final ServletContextHandler servletContextHandler;

    @Inject
    public ServletContextHandlerFactory(Server server,
                                        Configuration configuration,
                                        ServiceLocator serviceLocator,
                                        Provider<ConstraintSecurityHandler> securityHandlerProvider) {
        servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        servletContextHandler.setContextPath("/");
        servletContextHandler.setBaseResource(new ResourcesResource(""));

        JettyWebSocketServletContainerInitializer.configure(servletContextHandler, null);

        Handler handler = servletContextHandler;

        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setHandler(handler);
        handler = gzipHandler;

        ConstraintSecurityHandler securityHandler = securityHandlerProvider.get();
        if (securityHandler != null) {
            securityHandler.setHandler(handler);
            handler = securityHandler;
        }

        // Log4j2 ThreadContext handler
        Log4j2ThreadContextHandler log4J2ThreadContextHandler = new Log4j2ThreadContextHandler(configuration);
        log4J2ThreadContextHandler.setHandler(handler);

        server.setHandler(handler);
    }

    @Override
    @Singleton
    @Named("jetty.server")
    public ServletContextHandler provide() {
        return servletContextHandler;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
    }
}
