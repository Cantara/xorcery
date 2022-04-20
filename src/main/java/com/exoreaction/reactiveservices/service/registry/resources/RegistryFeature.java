package com.exoreaction.reactiveservices.service.registry.resources;

import com.exoreaction.reactiveservices.service.registry.resources.websocket.RegistryWebSocketServlet;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */

@Provider
public class RegistryFeature
    implements Feature, ContainerLifecycleListener
{
    @Override
    public boolean configure( FeatureContext context )
    {
        context.register( new RegistryService() );
        context.getConfiguration().
        ctx.addServlet( new ServletHolder( new RegistryWebSocketServlet( service ) ), "/ws/registryevents" );

        return false;
    }

    @Override
    public void onStartup( Container container )
    {

    }

    @Override
    public void onReload( Container container )
    {

    }

    @Override
    public void onShutdown( Container container )
    {

    }
}
