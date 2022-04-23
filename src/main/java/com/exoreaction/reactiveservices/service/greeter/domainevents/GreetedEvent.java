package com.exoreaction.reactiveservices.service.greeter.domainevents;

import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvent;

public class GreetedEvent
    implements DomainEvent
{
    public String greeting;
}
