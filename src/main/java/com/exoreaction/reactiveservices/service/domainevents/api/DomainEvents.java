package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;
import java.util.Map;

public record DomainEvents(List<DomainEvent> events) {

    public static DomainEvents events(DomainEvent... events) {
        return new DomainEvents(List.of(events));
    }

    @JsonValue
    @Override
    public List<DomainEvent> events() {
        return events;
    }
}
