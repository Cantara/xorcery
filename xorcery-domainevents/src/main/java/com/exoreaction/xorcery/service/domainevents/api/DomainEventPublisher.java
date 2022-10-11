package com.exoreaction.xorcery.service.domainevents.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.domainevents.api.aggregate.DomainEvents;
import org.glassfish.jersey.spi.Contract;

@Contract
public interface DomainEventPublisher
{
    void publish(Metadata metadata, DomainEvents events);
}
