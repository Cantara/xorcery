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
package dev.xorcery.jetty.server;


import dev.xorcery.configuration.Configuration;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.SecurityHandler;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.servlet")
@Priority(8)
public class ServletContextHandlerFactory
        implements Factory<ServletContextHandler> {
    private final ServletContextHandler servletContextHandler;

    @Inject
    public ServletContextHandlerFactory(Configuration configuration,
                                        ServiceLocator serviceLocator) {
        servletContextHandler = new ServletContextHandler();
        if (serviceLocator.<Object>getService(SessionHandler.class) instanceof SessionHandler sessionHandler)
        {
            servletContextHandler.setSessionHandler(sessionHandler);
        }
        if (serviceLocator.<Object>getService(ConstraintSecurityHandler.class) instanceof SecurityHandler securityHandler)
        {
            servletContextHandler.setSecurityHandler(securityHandler);
        }
        if (serviceLocator.<Object>getService(ErrorHandler.class) instanceof ErrorHandler errorHandler)
        {
            servletContextHandler.setErrorHandler(errorHandler);
        }
        servletContextHandler.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        servletContextHandler.setContextPath("/");
        // TODO More configuration
    }

    @Override
    @Singleton
    public ServletContextHandler provide() {
        return servletContextHandler;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
    }
}
