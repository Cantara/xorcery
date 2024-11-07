package dev.xorcery.domainevents.test.context;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record ThingContext(String id, ThingModel model, ServiceLocator thingSupplier)
        implements DomainContext {

    public record Factory(ServiceLocator thingSupplier) {
        @Inject
        public Factory {
        }

        ThingContext bind(String id, ThingModel model)
        {
            return new ThingContext(id, model, thingSupplier);
        }
    }

    @Override
    public List<Command> commands() {
        return List.of(new ThingCommands.UpdateThing(id, model.getFoo()), new ThingCommands.DeleteThing(id));
    }

    @Override
    public <T extends Command> CompletableFuture<CommandResult<T>> handle(CommandMetadata metadata, T command) {
        return thingSupplier.createAndInitialize(ThingEntity.class).handle(metadata, model.element(), command);
    }
}
