package dev.xorcery.test.context;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import dev.xorcery.domainevents.publisher.api.CommandHandler;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record ThingContext(String id, ThingModel model, ServiceLocator thingSupplier, CommandHandler commandHandler)
        implements DomainContext {

    public record Factory(ServiceLocator thingSupplier, CommandHandler commandHandler) {
        @Inject
        public Factory {
        }

        ThingContext bind(String id, ThingModel model)
        {
            return new ThingContext(id, model, thingSupplier, commandHandler);
        }
    }

    @Override
    public List<Command> commands() {
        return List.of(new ThingCommands.UpdateThing(id, model.getFoo()), new ThingCommands.DeleteThing(id));
    }

    @Override
    public CompletableFuture<CommandResult> apply(CommandMetadata metadata, Command command) {
        return commandHandler.handle(thingSupplier.createAndInitialize(ThingEntity.class), metadata, command);
    }
}
