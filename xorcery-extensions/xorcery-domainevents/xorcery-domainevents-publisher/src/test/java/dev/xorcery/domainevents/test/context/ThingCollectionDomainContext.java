package dev.xorcery.domainevents.test.context;

import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import dev.xorcery.domainevents.entity.Command;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ThingCollectionDomainContext
    implements DomainContext
{
    private final Validator validator;

    @Inject
    public ThingCollectionDomainContext(Validator validator) {
        this.validator = validator;
    }

    @Override
    public List<Command> commands() {
        return List.of(new ThingCommands.CreateThing("1234", ""));
    }

    @Override
    public <T extends Command> CompletableFuture<CommandResult<T>> handle(CommandMetadata metadata, T command) {
        Set<ConstraintViolation<T>> validationErrors = validator.validate(command);
        if (validationErrors.isEmpty())
            return new ThingEntity().handle(metadata, new ThingEntity.ThingSnapshot(command.id(), ""), command);
        else
            return CompletableFuture.failedFuture(new ConstraintViolationException(validationErrors));
    }
}
