package com.exoreaction.reactiveservices.disruptor;

import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;

import java.util.List;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class UnicastEventHandler<T>
    implements DefaultEventHandler<EventHolder<T>>
{
    private final List<EventHandler<EventHolder<T>>> consumers;

    public UnicastEventHandler(List<EventHandler<EventHolder<T>>> consumers )
    {
        this.consumers = consumers;
    }

    @Override
    public void onEvent( EventHolder<T> event, long sequence, boolean endOfBatch ) throws Exception
    {
        while (true)
        {
            try {
                EventHandler<EventHolder<T>> consumer = consumers.get(0);
                consumer.onEvent( event, sequence, endOfBatch );
                return;
            } catch (IndexOutOfBoundsException e)
            {
                // Try again
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    return;
                }
            } catch (Exception e)
            {
                // Try again
                LogManager.getLogger(LogManager.getLogger()).error("Could not send events", e);
            }
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
