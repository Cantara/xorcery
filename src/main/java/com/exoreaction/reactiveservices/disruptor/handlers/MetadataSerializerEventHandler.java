package com.exoreaction.reactiveservices.disruptor.handlers;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MetadataSerializerEventHandler<T>
    implements DefaultEventHandler<Event<T>>
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onEvent(Event<T> event, long sequence, boolean endOfBatch ) throws Exception
    {
        if (event.metadata != null && event.headers == null)
        {
            event.headers = ByteBuffer.wrap( objectMapper.writeValueAsBytes( event.metadata ));
        }
    }
}
