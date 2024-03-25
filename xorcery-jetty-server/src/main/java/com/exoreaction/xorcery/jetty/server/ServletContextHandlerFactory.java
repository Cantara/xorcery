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
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service
@Priority(8)
public class ServletContextHandlerFactory
        implements Factory<ServletContextHandler> {
    private final ServletContextHandler servletContextHandler;

    @Inject
    public ServletContextHandlerFactory(Configuration configuration,
                                        ServiceLocator serviceLocator) {
        servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        servletContextHandler.setContextPath("/");
        // TODO More configuration
    }

    @Override
    @Named("jetty.server.servlet")
    @Singleton
    public ServletContextHandler provide() {
        return servletContextHandler;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
    }
}
