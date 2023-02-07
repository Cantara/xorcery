package com.exoreaction.xorcery.service.jersey.server;

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
    }

    public void stop() {
        getApplicationHandler().onShutdown(this);
    }

    @Override
    public void destroy() {
        // The above already does the right thing, at the right phase of the shutdown
    }
}
