package com.exoreaction.xorcery.domainevents.api;

import com.exoreaction.xorcery.metadata.Metadata;

import java.util.ArrayList;
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

    // Strip away attributes and relationships for security reasons
    public CommandEvents cloneWithoutState()
    {
        List<DomainEvent> cleanedDomainEvents = new ArrayList<>(events.size());
        for (DomainEvent domainEvent : events) {
            if (domainEvent instanceof JsonDomainEvent jde)
            {
                JsonDomainEvent jsonDomainEvent = new JsonDomainEvent(jde.json().deepCopy());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.attributes.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.addedattributes.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.removedattributes.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.updatedrelationships.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.addedrelationships.name());
                jsonDomainEvent.json().remove(Model.JsonDomainEventModel.removedrelationships.name());
                cleanedDomainEvents.add(jsonDomainEvent);
            } else
            {
                cleanedDomainEvents.add(domainEvent);
            }
        }
        return new CommandEvents(metadata, cleanedDomainEvents);
    }
}
