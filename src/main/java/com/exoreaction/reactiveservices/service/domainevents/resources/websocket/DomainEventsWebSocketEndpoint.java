package com.exoreaction.reactiveservices.service.domainevents.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.EventHandlerResult;
import com.exoreaction.reactiveservices.service.domainevents.DomainEventHolder;
import com.exoreaction.reactiveservices.service.domainevents.api.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class DomainEventsWebSocketEndpoint
    implements WebSocketListener, EventHandler<DomainEventHolder>
{
    private final List<EventHandler<DomainEventHolder>> consumers;
    private EventHandlerResult<List<Record>, Metadata> eventHandlerResult;
    private Session session;
    private Semaphore semaphore = new Semaphore(0);
    private ObjectMapper mapper = new ObjectMapper();

    public DomainEventsWebSocketEndpoint(List<EventHandler<DomainEventHolder>> consumers, EventHandlerResult<List<Record>, Metadata> eventHandlerResult)
    {
        this.consumers = consumers;
        this.eventHandlerResult = eventHandlerResult;
    }

    public Semaphore getSemaphore()
    {
        return semaphore;
    }

    // WebSocket
    @Override
    public void onWebSocketBinary( byte[] payload, int offset, int len )
    {
        try {
            Metadata result = mapper.readValue( payload, offset, len, Metadata.class );
            eventHandlerResult.complete(result);
        } catch (IOException e) {
            LogManager.getLogger(getClass()).error("Could not deserialize result", e);
        }
    }

    @Override
    public void onWebSocketText( String message )
    {
        semaphore.release(Integer.parseInt( message ));
    }

    @Override
    public void onWebSocketClose( int statusCode, String reason )
    {
        consumers.remove( this );
    }

    @Override
    public void onWebSocketConnect( Session session )
    {
        this.session = session;
        consumers.add( this );
    }

    @Override
    public void onWebSocketError( Throwable cause )
    {
    }

    // EventHandler
    @Override
    public void onEvent( DomainEventHolder event, long sequence, boolean endOfBatch ) throws Exception
    {
        while (!semaphore.tryAcquire(1, TimeUnit.SECONDS))
        {
            if (!session.isOpen())
                return;
        }
        session.getRemote().sendBytes( event.headers, new WriteCallback()
        {
            @Override
            public void writeFailed( Throwable x )
            {
                // TODO
            }

            @Override
            public void writeSuccess()
            {
                session.getRemote().sendBytes( event.body, new WriteCallback()
                {
                    @Override
                    public void writeFailed( Throwable x )
                    {
                        // TODO
                    }

                    @Override
                    public void writeSuccess()
                    {
                        // TODO
                    }
                } );
            }
        } );
    }

    @Override
    public void onShutdown()
    {
        session.close();
    }
}
