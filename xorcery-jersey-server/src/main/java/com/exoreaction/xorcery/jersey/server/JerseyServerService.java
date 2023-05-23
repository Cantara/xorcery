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
package com.exoreaction.xorcery.jersey.server;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jersey3.MetricsFeature;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.jersey.server.resources.ServerApplication;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jersey.server")
@RunLevel(4)
public class JerseyServerService
        implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());

    private final JerseyServletContainer servletContainer;

    @Inject
    public JerseyServerService(Configuration configuration,
                               ServiceResourceObjects sro,
                               Provider<ServletContextHandler> ctxProvider, MetricRegistry metricRegistry) throws Exception {

        ServerApplication app = new ServerApplication(configuration);

        configuration.getList("jersey.server.register").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                try {
                    app.register(getClass().getClassLoader().loadClass(jsonNode.asText()));
                    logger.debug("Registered " + jsonNode.asText());
                } catch (ClassNotFoundException e) {
                    logger.error("Could not load JAX-RS provider " + jsonNode.asText(), e);
                    throw new RuntimeException(e);
                }
            }
        });

        if (metricRegistry != null) {
            app.register(new MetricsFeature(metricRegistry));
        }

        servletContainer = new JerseyServletContainer(app);
        ServletHolder servletHolder = new ServletHolder(servletContainer);
        servletHolder.setInitOrder(1);
        ServletContextHandler servletContextHandler = ctxProvider.get();
        servletContextHandler.addServlet(servletHolder, "/*");

        sro.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "server")
                .attribute("jetty.version", Jetty.VERSION) // TODO is this supposed to be jetty version here?
                .build());

        logger.info("Jersey started");
    }

    @Override
    public void preDestroy() {
        servletContainer.stop();
    }
}
