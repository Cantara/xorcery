package com.exoreaction.reactiveservices.service.soutmetrics;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.parser.JsonLogEventParser;

import java.io.StringReader;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MetricDeserializeEventHandler
    implements DefaultEventHandler<EventHolder<JsonObject>>
{
    public MetricDeserializeEventHandler()
    {
    }

    @Override
    public void onEvent( EventHolder<JsonObject> event, long sequence, boolean endOfBatch ) throws Exception
    {
        String json = new String(event.body.array());
        event.event = Json.createReader(new StringReader(json)).readObject();
    }
}
