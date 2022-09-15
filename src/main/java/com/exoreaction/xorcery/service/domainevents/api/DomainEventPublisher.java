package com.exoreaction.xorcery.service.domainevents.api;

import com.exoreaction.xorcery.service.domainevents.api.aggregate.DomainEvents;
import com.exoreaction.xorcery.metadata.Metadata;
import org.glassfish.jersey.spi.Contract;

import java.util.concurrent.CompletionStage;

@Contract
public interface DomainEventPublisher
{
    void publish(Metadata metadata, DomainEvents events);
}
