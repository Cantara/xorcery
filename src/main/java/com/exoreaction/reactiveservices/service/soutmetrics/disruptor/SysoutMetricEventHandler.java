package com.exoreaction.reactiveservices.service.soutmetrics.disruptor;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import jakarta.json.JsonObject;
import org.apache.logging.log4j.core.LogEvent;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class SysoutMetricEventHandler
    implements DefaultEventHandler<EventHolder<JsonObject>>
{
    @Override
    public void onEvent( EventHolder<JsonObject> event, long sequence, boolean endOfBatch ) throws Exception
    {
        System.out.println("Metric:"+event.event.toString()+":"+event.metadata);
    }
}
