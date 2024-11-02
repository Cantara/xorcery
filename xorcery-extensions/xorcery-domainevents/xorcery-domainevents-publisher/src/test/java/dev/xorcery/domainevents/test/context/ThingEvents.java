package dev.xorcery.domainevents.test.context;

import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.JsonDomainEvent.StateBuilder;

public interface ThingEvents {

    default StateBuilder createdThing(String id, String foo)
    {
        return JsonDomainEvent.event("CreatedThing" )
                .created("Thing", id)
                .updatedAttribute("foo", foo);
    }

    default StateBuilder updatedThing(String id, String foo)
    {
        return JsonDomainEvent.event("UpdatedThing" )
                .updated("Thing", id)
                .updatedAttribute("foo", foo);
    }

    default JsonDomainEvent deletedThing(String id)
    {
        return JsonDomainEvent.event("DeletedThing" )
                .deleted("Thing", id);
    }
}
