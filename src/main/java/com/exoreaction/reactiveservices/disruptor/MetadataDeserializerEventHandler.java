package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.util.MultiMap;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MetadataDeserializerEventHandler
    implements DefaultEventHandler<EventHolder<?>>
{
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void onEvent( EventHolder<?> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.metadata = mapper.readValue( event.headers.array(), MultiMap.class );
    }
}
