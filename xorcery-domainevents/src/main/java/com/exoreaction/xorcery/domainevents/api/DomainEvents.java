package com.exoreaction.xorcery.domainevents.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;

public record DomainEvents(List<DomainEvent> events) {

    public static DomainEvents of(DomainEvent... events) {
        List<DomainEvent> eventList = new ArrayList<>(events.length);
        Collections.addAll(eventList, events);
        return new DomainEvents(eventList);
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
