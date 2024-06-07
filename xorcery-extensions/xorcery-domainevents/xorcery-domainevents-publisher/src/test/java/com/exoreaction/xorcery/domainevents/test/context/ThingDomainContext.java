package com.exoreaction.xorcery.domainevents.test.context;

import com.exoreaction.xorcery.domainevents.context.CommandMetadata;
import com.exoreaction.xorcery.domainevents.context.CommandResult;
import com.exoreaction.xorcery.domainevents.context.DomainContext;
import com.exoreaction.xorcery.domainevents.entity.Command;
import com.exoreaction.xorcery.metadata.Metadata;
import jakarta.validation.ValidatorFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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
