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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jersey.server.resources.ServerApplication;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jersey.server")
@RunLevel(6)
public class JerseyServerService
        implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());

    private final JerseyServletContainer servletContainer;

    @Inject
    public JerseyServerService(Configuration configuration,
                               ServletContextHandler servletContextHandler) {

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

        servletContainer = new JerseyServletContainer(app);
        ServletHolder servletHolder = new ServletHolder("jersey", servletContainer);
        servletHolder.setInitOrder(1);
        servletContextHandler.addServlet(servletHolder, "/*");

        logger.info("Jersey started");
    }

    @Override
    public void preDestroy() {
        servletContainer.stop();
    }
}
