package com.exoreaction.reactiveservices.service.registry.resources.websocket;

import com.exoreaction.reactiveservices.service.registry.resources.RegistryService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public class RegistryEndpoint
    implements WebSocketListener
{
    private RegistryService service;
    private Session session;

    public RegistryEndpoint( RegistryService service )
    {
        this.service = service;
    }

    @Override
    public void onWebSocketBinary( byte[] payload, int offset, int len )
    {
    }

    @Override
    public void onWebSocketText( String message )
    {
    }

    @Override
    public void onWebSocketConnect( Session session )
    {
        this.session = session;
        service.addSession( session );
    }

    @Override
    public void onWebSocketClose( int statusCode, String reason )
    {
        service.removeSession( session );
    }

    @Override
    public void onWebSocketError( Throwable cause )
    {
    }
}
