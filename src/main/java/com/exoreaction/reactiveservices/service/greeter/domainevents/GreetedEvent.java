package com.exoreaction.reactiveservices.service.greeter.domainevents;

import com.exoreaction.reactiveservices.service.domainevents.spi.DomainEvent;

public class GreetedEvent
    implements DomainEvent
{
    public String greeting;
}
