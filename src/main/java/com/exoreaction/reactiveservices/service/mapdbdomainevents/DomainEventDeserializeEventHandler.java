package com.exoreaction.reactiveservices.service.mapdbdomainevents;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

import java.io.StringReader;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class DomainEventDeserializeEventHandler
    implements DefaultEventHandler<EventHolder<JsonArray>>
{
    public DomainEventDeserializeEventHandler()
    {
    }

    @Override
    public void onEvent( EventHolder<JsonArray> event, long sequence, boolean endOfBatch ) throws Exception
    {
        String json = new String(event.body.array());
        event.event = Json.createReader(new StringReader(json)).readArray();
    }
}
