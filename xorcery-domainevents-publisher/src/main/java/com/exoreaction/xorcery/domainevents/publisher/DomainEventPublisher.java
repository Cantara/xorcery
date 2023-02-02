package com.exoreaction.xorcery.domainevents.publisher;

import com.exoreaction.xorcery.domainevents.api.DomainEvents;
import com.exoreaction.xorcery.metadata.Metadata;

public interface DomainEventPublisher
{
    void publish(Metadata metadata, DomainEvents events);
}
