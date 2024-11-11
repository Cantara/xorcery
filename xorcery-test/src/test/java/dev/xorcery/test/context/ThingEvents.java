package dev.xorcery.test.context;

import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent.StateBuilder;

public interface ThingEvents {

    String id();

    default StateBuilder createdThing(String foo)
    {
        return JsonDomainEvent.event("CreatedThing" )
                .created("Thing", id())
                .updatedAttribute("foo", foo);
    }

    default StateBuilder updatedThing(String foo)
    {
        return JsonDomainEvent.event("UpdatedThing" )
                .updated("Thing", id())
                .updatedAttribute("foo", foo);
    }

    default JsonDomainEvent deletedThing()
    {
        return JsonDomainEvent.event("DeletedThing" )
                .deleted("Thing", id());
    }
}
