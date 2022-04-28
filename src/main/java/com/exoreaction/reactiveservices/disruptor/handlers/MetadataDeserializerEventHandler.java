package com.exoreaction.reactiveservices.disruptor.handlers;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MetadataDeserializerEventHandler
    implements DefaultEventHandler<Event<?>>
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void onEvent(Event<?> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.metadata = mapper.readValue( event.headers.array(), Metadata.class );
    }
}
