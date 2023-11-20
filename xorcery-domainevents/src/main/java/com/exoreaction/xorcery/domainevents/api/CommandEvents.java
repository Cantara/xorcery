package com.exoreaction.xorcery.domainevents.api;

import com.exoreaction.xorcery.metadata.Metadata;

import java.util.List;

public class CommandEvents {
    private Metadata metadata;
    private List<DomainEvent> events;

    public CommandEvents() {
    }

    public CommandEvents(Metadata metadata, List<DomainEvent> events) {
        this.metadata = metadata;
        this.events = events;
    }

    public void set(Metadata metadata, List<DomainEvent> events) {
        this.metadata = metadata;
        this.events = events;
    }

    public void set(CommandEvents other) {
        this.metadata = other.metadata;
        this.events = other.events;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public List<DomainEvent> getEvents() {
        return events;
    }

}
