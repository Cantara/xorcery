package com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.StringReader;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class DomainEventDeserializeEventHandler
    implements DefaultEventHandler<Event<JsonObject>>
{
    public DomainEventDeserializeEventHandler()
    {
    }

    @Override
    public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch ) throws Exception
    {
        String json = new String(event.body.array());
        event.event = Json.createReader(new StringReader(json)).readObject();
    }
}
