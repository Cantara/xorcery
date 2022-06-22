package com.exoreaction.xorcery.service.domainevents;

import com.exoreaction.xorcery.cqrs.aggregate.DomainEvents;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.EventWithResult;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;

public class DomainEventHolder
    extends Event<EventWithResult<DomainEvents, Metadata>>
{
}
