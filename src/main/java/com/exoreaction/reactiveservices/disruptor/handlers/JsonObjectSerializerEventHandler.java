package com.exoreaction.reactiveservices.disruptor.handlers;

import com.exoreaction.reactiveservices.disruptor.Event;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class JsonObjectSerializerEventHandler
    implements DefaultEventHandler<Event<JsonObject>>
{
    @Override
    public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch) throws Exception
    {
        if (event.event != null && event.body == null)
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            Json.createWriter(out).write(event.event);
            event.body = ByteBuffer.wrap(out.toByteArray());
        }
    }
}
