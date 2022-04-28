package com.exoreaction.reactiveservices.disruptor.handlers;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;

public class ObjectMapperSerializeEventHandler
        implements DefaultEventHandler<Event<DomainEvents>>
{
    private final ObjectMapper objectMapper;

    public ObjectMapperSerializeEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent(Event<DomainEvents> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.body = ByteBuffer.wrap(objectMapper.writeValueAsBytes(event.event));
    }
}
