package com.exoreaction.reactiveservices.service.registry.resources.websocket;

import com.exoreaction.reactiveservices.service.registry.resources.RegistryService;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class RegistryWebSocketServlet
    extends JettyWebSocketServlet
{
    private RegistryService registryService;

    public RegistryWebSocketServlet( RegistryService registryService )
    {
        this.registryService = registryService;
    }

    @Override
    protected void configure( JettyWebSocketServletFactory factory )
    {
        factory.setMaxTextMessageSize( 1048576 );

        factory.addMapping( "/ws/registryevents",
            (( request, response ) -> new RegistryEndpoint( registryService )) );
    }
}
