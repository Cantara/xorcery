package com.exoreaction.reactiveservices.service.soutmetrics.disruptor;

import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import jakarta.json.JsonObject;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class SysoutMetricEventHandler
    implements DefaultEventHandler<Event<JsonObject>>
{
    @Override
    public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch ) throws Exception
    {
        System.out.println("Metric:"+event.event.toString()+":"+event.metadata);
    }
}
