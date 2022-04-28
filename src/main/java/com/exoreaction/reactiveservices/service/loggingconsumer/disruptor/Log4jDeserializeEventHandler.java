package com.exoreaction.reactiveservices.service.loggingconsumer.disruptor;

import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Log4jDeserializeEventHandler
    implements DefaultEventHandler<Event<LogEvent>>
{
    private final JsonLogEventParser parser;

    public Log4jDeserializeEventHandler( JsonLogEventParser parser )
    {
        this.parser = parser;
    }

    @Override
    public void onEvent(Event<LogEvent> event, long sequence, boolean endOfBatch ) throws Exception
    {
        if (event.event == null && event.body != null)
            event.event = parser.parseFrom( event.body.array());
    }
}
