package com.exoreaction.reactiveservices.service.conductor.resources.websocket;

import com.exoreaction.reactiveservices.service.conductor.resources.ConductorService;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public class ConductorEndpoint
    implements WebSocketListener
{
    private ConductorService service;
    private Session session;

    public ConductorEndpoint( ConductorService service )
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
