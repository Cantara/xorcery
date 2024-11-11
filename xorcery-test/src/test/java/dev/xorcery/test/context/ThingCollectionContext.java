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

public class ThingCollectionContext
        implements DomainContext {
    private final ServiceLocator thingSupplier;
    private final CommandHandler commandHandler;

    @Inject
    public ThingCollectionContext(ServiceLocator thingSupplier, CommandHandler commandHandler) {
        this.thingSupplier = thingSupplier;
        this.commandHandler = commandHandler;
    }


    @Override
    public List<Command> commands() {
        return List.of(new ThingCommands.CreateThing("1234", ""));
    }

    @Override
    public CompletableFuture<CommandResult> apply(CommandMetadata metadata, Command command) {
        return commandHandler.handle(thingSupplier.createAndInitialize(ThingEntity.class), metadata, command);
    }
}
