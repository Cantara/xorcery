package com.exoreaction.reactiveservices.service.greeter.domainevents;

import com.exoreaction.reactiveservices.cqrs.aggregate.DomainEvent;

public record UpdatedGreeting(String greeting)
    implements DomainEvent
{
}
