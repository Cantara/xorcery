package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MetadataSerializerEventHandler
    implements DefaultEventHandler<EventHolder<?>>
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onEvent( EventHolder<?> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.headers = ByteBuffer.wrap( objectMapper.writeValueAsBytes( event.metadata ));
    }
}
