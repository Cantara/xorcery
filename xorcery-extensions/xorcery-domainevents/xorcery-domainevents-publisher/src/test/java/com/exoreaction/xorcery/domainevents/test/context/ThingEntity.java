package com.exoreaction.xorcery.domainevents.test.context;

import com.exoreaction.xorcery.domainevents.entity.Entity;

import static com.exoreaction.xorcery.domainevents.test.context.ThingCommands.*;

public class ThingEntity
    extends Entity<ThingEntity.ThingSnapshot>
    implements ThingEvents
{
    public record ThingSnapshot(String id, String foo){
    }

    public void handle(CreateThing command)
    {
        event(createdThing(command.id(), command.foo()).build());
    }

    public void handle(UpdateThing command)
    {
        event(updatedThing(command.id(), command.foo()).build());
    }

    public void handle(DeleteThing command)
    {
        event(deletedThing(command.id()));
    }
}
