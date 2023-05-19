/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
