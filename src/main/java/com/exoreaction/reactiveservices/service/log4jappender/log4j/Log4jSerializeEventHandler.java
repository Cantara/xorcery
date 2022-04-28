package com.exoreaction.reactiveservices.service.log4jappender.log4j;

import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.JsonLayout;

import java.nio.ByteBuffer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Log4jSerializeEventHandler
    implements DefaultEventHandler<Event<LogEvent>>
{
    private final JsonLayout layout;

    public Log4jSerializeEventHandler( JsonLayout layout )
    {
        this.layout = layout;
    }

    @Override
    public void onEvent(Event<LogEvent> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.body = ByteBuffer.wrap(layout.toByteArray( event.event ));
    }
}
