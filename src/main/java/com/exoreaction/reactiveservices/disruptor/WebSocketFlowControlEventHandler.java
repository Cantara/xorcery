package com.exoreaction.reactiveservices.disruptor;

import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class WebSocketFlowControlEventHandler
    implements DefaultEventHandler<Object>
{
    private final Session session;
    private long batchSize;

    public WebSocketFlowControlEventHandler( long initialRequest, Session session ) throws IOException
    {
        this.session = session;
        session.getRemote().sendString( Long.toString( initialRequest ) );
    }

    @Override
    public void onEvent( Object event, long sequence, boolean endOfBatch ) throws Exception
    {
        if (endOfBatch)
        {
            session.getRemote().sendString( Long.toString( batchSize ) );
        }
        DefaultEventHandler.super.onEvent( event, sequence, endOfBatch );
    }

    @Override
    public void onBatchStart( long batchSize )
    {
        this.batchSize = batchSize;
    }
}
