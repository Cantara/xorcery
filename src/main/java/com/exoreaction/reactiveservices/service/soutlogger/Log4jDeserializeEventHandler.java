package com.exoreaction.reactiveservices.service.soutlogger;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class Log4jDeserializeEventHandler
    implements DefaultEventHandler<EventHolder<LogEvent>>
{
    private final JsonLogEventParser parser;

    public Log4jDeserializeEventHandler( JsonLogEventParser parser )
    {
        this.parser = parser;
    }

    @Override
    public void onEvent( EventHolder<LogEvent> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.event = parser.parseFrom( event.body.array());
    }
}
