package com.exoreaction.reactiveservices.service.registry.resources;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public class RegistryService
{
    private final List<ResourceDocument> servers = new CopyOnWriteArrayList<>();
    private List<Session> sessions = new CopyOnWriteArrayList<>();

    public void addServer( ResourceDocument server )
    {
        servers.add( server );

        send( server );

        LogManager.getLogger(getClass()).info("Added server:"+server);
    }

    public void removeServer( String serverSelfUri )
    {
        servers.removeIf( resourceDocument -> resourceDocument.getLinks().getSelf()
                                                              .map( link -> link.getHref().equals( serverSelfUri ) )
                                                              .orElse( false ) );
    }

    public List<ResourceDocument> getServers()
    {
        return servers;
    }

    public void addSession( Session session )
    {
        sessions.add( session );
    }

    public void removeSession( Session session )
    {
        sessions.remove( session );
    }

    public void send( ResourceDocument resourceObject )
    {
        final byte[] msg = resourceObject.toString().getBytes( StandardCharsets.UTF_8 );
        var byteBuffer = ByteBuffer.wrap( msg );

        for ( int i = 0; i < sessions.size(); i++ )
        {
            Session session = sessions.get( i );
            try
            {
                session.getRemote().sendBytes( byteBuffer );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }
}
