package com.exoreaction.xorcery.service.domainevents.api.entity;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

public record DomainEvents(List<DomainEvent> events) {

    public static DomainEvents of(DomainEvent... events) {
        return new DomainEvents(List.of(events));
    }

    @JsonValue
    @Override
    public List<DomainEvent> events() {
        return events;
    }
}
