package dev.xorcery.domainevents.test.context;

import dev.xorcery.collections.MapElement;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ThingCollectionContext
        implements DomainContext {
    private final ServiceLocator thingSupplier;

    @Inject
    public ThingCollectionContext(ServiceLocator thingSupplier) {
        this.thingSupplier = thingSupplier;
    }


    @Override
    public List<Command> commands() {
        return List.of(new ThingCommands.CreateThing("1234", ""));
    }

    @Override
    public <T extends Command> CompletableFuture<CommandResult<T>> handle(CommandMetadata metadata, T command) {
        return thingSupplier.create(ThingEntity.class).handle(metadata, MapElement.element(Collections.emptyMap()), command);
    }
}
