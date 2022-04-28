package com.exoreaction.reactiveservices.disruptor.handlers;

import com.lmax.disruptor.EventHandler;

import java.util.List;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class BroadcastEventHandler<T>
    implements DefaultEventHandler<T>
{
    private final List<EventHandler<T>> consumers;

    public BroadcastEventHandler( List<EventHandler<T>> consumers )
    {
        this.consumers = consumers;
    }

    @Override
    public void onEvent( T event, long sequence, boolean endOfBatch ) throws Exception
    {
        for ( EventHandler<T> consumer : consumers )
        {
            consumer.onEvent( event, sequence, endOfBatch );
        }
    }

    @Override
    public void onStart()
    {
        for ( EventHandler<T> consumer : consumers )
        {
            consumer.onStart();
        }
    }

    @Override
    public void onShutdown()
    {
        for ( EventHandler<T> consumer : consumers )
        {
            consumer.onShutdown();
        }
    }
}
