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


import dev.xorcery.hk2.Services;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.Optional;

@Service(name = "jetty.server")
@RunLevel(18)
public class JettyLifecycleService
        implements PreDestroy {

    private final Logger logger;
    private final Server server;

    @Inject
    public JettyLifecycleService(
            Server server,
            ServiceLocator serviceLocator,
            Logger logger) throws Exception {
        this.server = server;
        this.logger = logger;

        // Register all handlers
        List<ServiceHandle<Handler>> rankedHandlers = Services.allOfTypeRanked(serviceLocator, Handler.class)
                .filter(sh -> !(sh.getService() instanceof Server))
                .toList();

        Handler chain = null;
        for (ServiceHandle<Handler> serviceHandle : rankedHandlers) {
            logger.debug("Registering handler:" + Optional.ofNullable(serviceHandle.getActiveDescriptor().getName()).orElse(serviceHandle.getActiveDescriptor().getImplementation()));
            Handler handler = serviceHandle.getService();

            if (handler instanceof SessionHandler || handler instanceof ConstraintSecurityHandler || handler instanceof ErrorHandler)
                continue;

            if (chain == null) {
                chain = handler;
            } else if (handler instanceof ServletContextHandler servletContextHandler &&
                    chain instanceof Handler.Singleton singleton) {
                servletContextHandler.insertHandler(singleton);
                chain = servletContextHandler;
            } else if (handler instanceof Handler.Wrapper wrapper) {
                wrapper.setHandler(chain);
                chain = wrapper;
            }
        }
        if (chain != null)
            server.setHandler(chain);

        server.start();
        logger.info("Started Jetty server");
    }

    @Override
    public void preDestroy() {
        logger.info("Stopping Jetty server");
        try {
            server.stop();
        } catch (Throwable e) {
            logger.error(e);
        }
    }
}
