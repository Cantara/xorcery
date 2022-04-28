package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.handlers.MetadataSerializerEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.lmax.disruptor.AggregateEventHandler;
import com.lmax.disruptor.EventHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class ReactiveStreamWebSocketEndpoint<T>
    implements WebSocketListener, ReactiveEventStreams.Subscriber<T>, EventHandler<Event<T>>
{
    private Session session;
    private Semaphore semaphore = new Semaphore(0);
    private ReactiveEventStreams.Publisher<T> publisher;
    private Map<String, String> parameters;
    private EventHandler<Event<T>> serializer;
    private ReactiveEventStreams.Subscription subscription;

    public ReactiveStreamWebSocketEndpoint(ReactiveEventStreams.Publisher<T> publisher,
                                           Map<String, String> parameters,
                                           EventHandler<Event<T>> serializer)
    {
        this.publisher = publisher;
        this.parameters = parameters;
        this.serializer = serializer;
    }

    public Semaphore getSemaphore()
    {
        return semaphore;
    }

    // WebSocket
    @Override
    public void onWebSocketBinary( byte[] payload, int offset, int len )
    {
        // TODO
        // Handle CompletionStage
    }

    @Override
    public void onWebSocketText( String message )
    {
        long requestAmount = Long.parseLong(message);
        semaphore.release((int)requestAmount);
        subscription.request(requestAmount);
    }

    @Override
    public void onWebSocketClose( int statusCode, String reason )
    {
        subscription.cancel();
    }

    @Override
    public void onWebSocketConnect( Session session )
    {
        this.session = session;
        publisher.subscribe( this,parameters );
    }

    @Override
    public void onWebSocketError( Throwable cause )
    {
    }

    // Subscriber
    @Override
    public EventHandler<Event<T>> onSubscribe(ReactiveEventStreams.Subscription subscription) {
        this.subscription = subscription;
        return new AggregateEventHandler<>(serializer, new MetadataSerializerEventHandler<>(), this);
    }

    // EventHandler
    @Override
    public void onEvent(Event<T> event, long sequence, boolean endOfBatch ) throws Exception
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
