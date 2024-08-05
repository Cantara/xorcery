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
        add(createdThing(command.id(), command.foo()).build());
    }

    public void handle(UpdateThing command)
    {
        add(updatedThing(command.id(), command.foo()).build());
    }

    public void handle(DeleteThing command)
    {
        add(deletedThing(command.id()));
    }
}
