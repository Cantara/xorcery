package com.exoreaction.reactiveservices.service.domainevents.api;

import java.util.List;

public record DomainEvents(Metadata metadata, List<Record> events) {
    public DomainEvents(Metadata metadata, Record... events) {
        this(metadata, List.of(events));
    }
}
