package com.exoreaction.reactiveservices.disruptor.handlers;

import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.concurrent.Executor;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class WebSocketFlowControlEventHandler
    implements DefaultEventHandler<Object>
{
    private final Session session;
    private Executor flowControlExecutor;
    private long batchSize;

    public WebSocketFlowControlEventHandler( long initialRequest, Session session, Executor flowControlExecutor ) throws IOException
    {
        this.session = session;
        this.flowControlExecutor = flowControlExecutor;
        session.getRemote().sendString( Long.toString( initialRequest ) );
    }

    @Override
    public void onEvent( Object event, long sequence, boolean endOfBatch ) throws Exception
    {
        if (endOfBatch)
        {
            flowControlExecutor.execute(()->
                    {
                        try {
                            session.getRemote().sendString( Long.toString( batchSize ) );
                        } catch (IOException e) {
                            // TODO Error handling
                        }
                    }
                    );
        }
        DefaultEventHandler.super.onEvent( event, sequence, endOfBatch );
    }

    @Override
    public void onBatchStart( long batchSize )
    {
        this.batchSize = batchSize;
    }
}
