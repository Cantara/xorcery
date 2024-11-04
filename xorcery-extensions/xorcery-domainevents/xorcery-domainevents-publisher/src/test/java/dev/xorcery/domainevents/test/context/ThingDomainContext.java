package dev.xorcery.domainevents.test.context;

import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import dev.xorcery.domainevents.entity.Command;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ThingDomainContext
    implements DomainContext
{
    ThingEntity.ThingSnapshot snapshot;

    public void bind(ThingEntity.ThingSnapshot snapshot)
    {
        this.snapshot = snapshot;
    }

    @Override
    public List<Command> commands() {
        return List.of(new ThingCommands.UpdateThing(snapshot.id(), snapshot.foo()), new ThingCommands.DeleteThing(snapshot.id()));
    }

    @Override
    public <T extends Command> CompletableFuture<CommandResult<T>> handle(CommandMetadata metadata, T command) {
        return new ThingEntity().handle(metadata, snapshot, command);
    }
}