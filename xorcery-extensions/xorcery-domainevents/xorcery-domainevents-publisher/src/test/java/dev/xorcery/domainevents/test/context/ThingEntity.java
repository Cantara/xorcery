package dev.xorcery.domainevents.test.context;

import dev.xorcery.domainevents.entity.Entity;

import static dev.xorcery.domainevents.test.context.ThingCommands.*;

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
