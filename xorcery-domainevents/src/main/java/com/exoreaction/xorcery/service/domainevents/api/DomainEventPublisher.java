package com.exoreaction.xorcery.service.domainevents.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.domainevents.api.entity.DomainEvents;

public interface DomainEventPublisher
{
    void publish(Metadata metadata, DomainEvents events);
}
