package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.CompletionStage;

@Contract
public interface DomainEventPublisher
{
    CompletionStage<Metadata> publish(DomainEvents events);
}
