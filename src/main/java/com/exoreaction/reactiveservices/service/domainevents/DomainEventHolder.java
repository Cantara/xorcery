package com.exoreaction.reactiveservices.service.domainevents;

import com.exoreaction.reactiveservices.cqrs.aggregate.DomainEvents;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

public class DomainEventHolder
    extends Event<EventWithResult<DomainEvents, Metadata>>
{
}
