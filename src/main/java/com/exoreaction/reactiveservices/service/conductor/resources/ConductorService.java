package com.exoreaction.reactiveservices.service.conductor.resources;

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
 * @since 15/04/2022
 */
public class ConductorService
{
    private final List<ResourceDocument> patterns = new CopyOnWriteArrayList<>();
    private List<Session> sessions = new CopyOnWriteArrayList<>();

    public void addPattern( ResourceDocument pattern )
    {
        patterns.add( pattern );

        send( pattern );

        LogManager.getLogger(getClass()).info( "Added pattern:" + pattern);
    }

    public void removePattern( String patternSelfUri )
    {
        patterns.removeIf( resourceDocument -> resourceDocument.getLinks().getSelf()
                                                              .map( link -> link.getHref().equals( patternSelfUri ) )
                                                              .orElse( false ) );
    }

    public List<ResourceDocument> getPatterns()
    {
        return patterns;
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
