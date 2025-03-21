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
package dev.xorcery.jersey.server;

import org.apache.logging.log4j.LogManager;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * Overrides destroy so that Jetty shutdown doesn't perform the servlet lifecycle call.
 * This has to happen on shutdown of the Jersey service, so that the child service locator
 */
public class JerseyServletContainer
        extends ServletContainer
{

    public JerseyServletContainer(ResourceConfig resourceConfig) {
        super(resourceConfig);
        LogManager.getLogger().debug("Start");
    }

    public void stop() {
        LogManager.getLogger().debug("Stop");
        getApplicationHandler().onShutdown(this);
    }

    @Override
    public void destroy() {
        // The above already does the right thing, at the right phase of the shutdown
    }
}
