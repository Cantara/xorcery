package dev.xorcery.test.context;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.entity.Entity;
import jakarta.inject.Inject;

import static dev.xorcery.test.context.ThingCommands.*;

public class ThingEntity
    extends Entity
    implements ThingEvents
{
    private ThingModel snapshot;

    @Inject
    public ThingEntity() {
    }

    @Override
    protected void before(Command command) throws Exception {
        snapshot = new ThingModel(super.snapshot);
    }

    public void handle(CreateThing command)
    {
        add(createdThing(command.foo()).build());
    }

    public void handle(UpdateThing command)
    {
        if (snapshot.getFoo().equals(command.foo()))
            return;

        add(updatedThing(command.foo()).build());
    }

    public void handle(DeleteThing command)
    {
        add(deletedThing());
    }
}
