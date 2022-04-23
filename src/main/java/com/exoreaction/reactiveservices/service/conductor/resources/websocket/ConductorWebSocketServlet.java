package com.exoreaction.reactiveservices.service.conductor.resources.websocket;

import com.exoreaction.reactiveservices.service.conductor.resources.ConductorService;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */

public class ConductorWebSocketServlet
    extends JettyWebSocketServlet
{
    private ConductorService conductorService;

    public ConductorWebSocketServlet( ConductorService conductorService )
    {
        this.conductorService = conductorService;
    }

    @Override
    protected void configure( JettyWebSocketServletFactory factory )
    {
        factory.setMaxTextMessageSize( 1048576 );

        factory.addMapping( "/ws/conductorevents",
            (( request, response ) -> new ConductorEndpoint( conductorService )) );
    }
}
