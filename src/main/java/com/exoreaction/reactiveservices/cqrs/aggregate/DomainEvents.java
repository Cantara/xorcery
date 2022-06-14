package com.exoreaction.reactiveservices.cqrs.aggregate;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

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
