package com.exoreaction.reactiveservices.service.soutlogger.disruptor;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import org.apache.logging.log4j.core.LogEvent;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class SysoutLogEventHandler
    implements DefaultEventHandler<EventHolder<LogEvent>>
{
    @Override
    public void onEvent( EventHolder<LogEvent> event, long sequence, boolean endOfBatch ) throws Exception
    {
        System.out.println("Log:"+event.event.toString()+":"+event.metadata);
    }
}
