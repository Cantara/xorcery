package com.exoreaction.reactiveservices.disruptor.handlers;

import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringReader;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class JsonObjectDeserializeEventHandler
    implements DefaultEventHandler<Event<JsonObject>>
{
    public JsonObjectDeserializeEventHandler()
    {
    }

    @Override
    public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch ) throws Exception
    {
        if (event.body != null && event.event == null)
        {
            String json = new String(event.body.array());
            event.event = Json.createReader(new StringReader(json)).readObject();
        }
    }
}
