package com.exoreaction.xorcery.service.greeter.domainevents;

import com.exoreaction.xorcery.cqrs.aggregate.DomainEvent;

public record UpdatedGreeting(String greeting)
    implements DomainEvent
{
}
