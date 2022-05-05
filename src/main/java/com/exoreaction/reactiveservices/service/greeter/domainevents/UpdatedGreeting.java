package com.exoreaction.reactiveservices.service.greeter.domainevents;

import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvent;

public record UpdatedGreeting(String greeting)
    implements DomainEvent
{
}
