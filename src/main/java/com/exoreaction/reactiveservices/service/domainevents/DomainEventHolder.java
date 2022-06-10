package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.cqrs.DomainEvents;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;

public class DomainEventHolder
    extends Event<EventWithResult<DomainEvents, Metadata>>
{
}
