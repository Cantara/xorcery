package com.exoreaction.reactiveservices.service.domainevents.api;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface DomainEventPublisher
{
    CompletionStage<Metadata> publish(Metadata metadata, List<DomainEvent> events);
}
