package com.exoreaction.reactiveservices.service.log4jappender.log4j;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;

import java.nio.ByteBuffer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Log4jSerializeEventHandler
    implements DefaultEventHandler<EventHolder<LogEvent>>
{
    private final JsonLayout layout;

    public Log4jSerializeEventHandler( JsonLayout layout )
    {
        this.layout = layout;
    }

    @Override
    public void onEvent( EventHolder<LogEvent> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.body = ByteBuffer.wrap(layout.toByteArray( event.event ));
    }
}
