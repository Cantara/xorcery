package dev.xorcery.domainevents.publisher.spi;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.entity.Entity;

import java.util.concurrent.CompletableFuture;

public interface EntitySnapshotProvider {
    CompletableFuture<Snapshot> snapshotFor(CommandMetadata metadata, Command command, Entity entity);
    CompletableFuture<Boolean> snapshotExists(CommandMetadata metadata, Command command, Entity entity);
}
