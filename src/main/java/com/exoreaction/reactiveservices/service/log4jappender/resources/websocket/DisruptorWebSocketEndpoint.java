package com.exoreaction.reactiveservices.service.log4jappender.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.lmax.disruptor.EventHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class DisruptorWebSocketEndpoint<T>
    implements WebSocketListener, EventHandler<EventHolder<T>>
{
    private final List<EventHandler<EventHolder<T>>> consumers;
    private Session session;
    private Semaphore semaphore = new Semaphore(0);

    public DisruptorWebSocketEndpoint( List<EventHandler<EventHolder<T>>> consumers )
    {
        this.consumers = consumers;
    }

    public Semaphore getSemaphore()
    {
        return semaphore;
    }

    // WebSocket
    @Override
    public void onWebSocketBinary( byte[] payload, int offset, int len )
    {
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
    public void onEvent( EventHolder<T> event, long sequence, boolean endOfBatch ) throws Exception
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
