package com.exoreaction.reactiveservices.service.domainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.ByteBuffer;
import java.util.List;

public class DomainEventSerializeEventHandler
        implements DefaultEventHandler<EventHolder<List<Record>>>
{
    private final ObjectMapper objectMapper;

    public DomainEventSerializeEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent( EventHolder<List<Record>> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.body = ByteBuffer.wrap(objectMapper.writeValueAsBytes(event.event));
    }
}
