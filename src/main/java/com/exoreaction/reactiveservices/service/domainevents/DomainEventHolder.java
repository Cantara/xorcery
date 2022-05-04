package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;

public class DomainEventHolder
    extends Event<EventWithResult<DomainEvents, Metadata>>
{
}
