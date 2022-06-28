package com.exoreaction.xorcery.disruptor.handlers;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.disruptor.Event;
import com.lmax.disruptor.EventSink;

import java.util.List;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class BroadcastEventHandler<T>
    implements DefaultEventHandler<Event<T>>
{
    private final List<EventSink<Event<T>>> subscribers;

    public BroadcastEventHandler( List<EventSink<Event<T>>> subscribers)
    {
        this.subscribers = subscribers;
    }

    @Override
    public void onEvent( Event<T> event, long sequence, boolean endOfBatch ) throws Exception
    {
        for ( EventSink<Event<T>> subscriber : subscribers)
        {
            subscriber.publishEvent((e, seq, e2) ->
            {
                e.metadata = new Metadata.Builder().add(e2.metadata).build();
                e.event = e2.event;
            }, event);
        }
    }

    @Override
    public void onStart()
    {
    }

    @Override
    public void onShutdown()
    {
    }
}
