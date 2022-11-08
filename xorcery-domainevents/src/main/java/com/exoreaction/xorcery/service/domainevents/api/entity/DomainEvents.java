package com.exoreaction.xorcery.service.domainevents.api.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;

public record DomainEvents(List<DomainEvent> events) {

    public static DomainEvents of(DomainEvent... events) {
        return new DomainEvents(List.of(events));
    }

    @JsonCreator(mode = DELEGATING)
    public DomainEvents {
    }

    @JsonValue
    @Override
    public List<DomainEvent> events() {
        return events;
    }
}
