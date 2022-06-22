package com.exoreaction.xorcery.disruptor.handlers;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.Sequence;

/**
 * Default no-op Disruptor EventHandler
 *
 * @author rickardoberg
 * @since 18/04/2022
 */

public interface DefaultEventHandler<T>
    extends EventHandler<T>
{
    @Override
    default void onEvent( T event, long sequence, boolean endOfBatch ) throws Exception
    {

    }

    @Override
    default void onBatchStart( long batchSize )
    {
        EventHandler.super.onBatchStart( batchSize );
    }

    @Override
    default void onStart()
    {
    }

    @Override
    default void onShutdown()
    {
    }

    @Override
    default void setSequenceCallback( Sequence sequenceCallback )
    {
    }

    @Override
    default void onTimeout( long sequence ) throws Exception
    {
    }
}
