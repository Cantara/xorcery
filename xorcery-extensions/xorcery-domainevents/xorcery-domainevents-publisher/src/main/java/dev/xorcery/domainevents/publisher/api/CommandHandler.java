package dev.xorcery.domainevents.publisher.api;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.entity.Entity;

import java.util.concurrent.CompletableFuture;

public interface CommandHandler {
    CompletableFuture<CommandResult> handle(Entity entity, CommandMetadata metadata, Command command);
}
