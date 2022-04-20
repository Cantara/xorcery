package com.exoreaction.reactiveservices.disruptor;

import com.lmax.disruptor.EventHandler;

import java.util.List;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class BroadcastEventHandler<T>
    implements DefaultEventHandler<EventHolder<T>>
{
    private final List<EventHandler<EventHolder<T>>> consumers;

    public BroadcastEventHandler( List<EventHandler<EventHolder<T>>> consumers )
    {
        this.consumers = consumers;
    }

    @Override
    public void onEvent( EventHolder<T> event, long sequence, boolean endOfBatch ) throws Exception
    {
        for ( EventHandler<EventHolder<T>> consumer : consumers )
        {
            consumer.onEvent( event, sequence, endOfBatch );
        }
    }

    @Override
    public void onStart()
    {
        for ( EventHandler<EventHolder<T>> consumer : consumers )
        {
            consumer.onStart();
        }
    }

    @Override
    public void onShutdown()
    {
        for ( EventHandler<EventHolder<T>> consumer : consumers )
        {
            consumer.onShutdown();
        }
    }
}
