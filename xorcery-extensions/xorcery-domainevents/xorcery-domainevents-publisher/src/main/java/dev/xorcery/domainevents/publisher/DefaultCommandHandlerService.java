/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.domainevents.publisher;

import dev.xorcery.collections.MapElement;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.entity.Entity;
import dev.xorcery.domainevents.entity.EntityWrongVersionException;
import dev.xorcery.domainevents.publisher.api.CommandHandler;
import dev.xorcery.domainevents.publisher.api.DomainEventPublisher;
import dev.xorcery.domainevents.publisher.spi.EntitySnapshotProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static dev.xorcery.domainevents.api.DomainEventMetadata.timestamp;

@Service(name="domainevents.commandhandler.default")
@ContractsProvided(CommandHandler.class)
public class DefaultCommandHandlerService
        implements CommandHandler {
    private final EntitySnapshotProvider entitySnapshotProvider;
    private final DomainEventPublisher domainEventPublisher;

    @Inject
    public DefaultCommandHandlerService(EntitySnapshotProvider entitySnapshotProvider, DomainEventPublisher domainEventPublisher) {
        this.entitySnapshotProvider = entitySnapshotProvider;
        this.domainEventPublisher = domainEventPublisher;
    }

    public CompletableFuture<CommandResult> handle(Entity entity, CommandMetadata metadata, Command command) {

        if (!metadata.context().has(timestamp))
        {
            metadata.context().toBuilder().add(timestamp, System.currentTimeMillis());
        }

        if (Command.isCreate(command.getClass())) {
            return entitySnapshotProvider.snapshotExists(metadata, command, entity)
                    .thenCompose(exists -> exists
                            ? CompletableFuture.failedFuture(new IllegalStateException("Entity already exists"))
                            : entity.handle(metadata, MapElement.element(Collections.emptyMap()), command)
                            .thenCompose(result -> domainEventPublisher.publish(new MetadataEvents(result.metadata(), result.events()))
                                    .thenApply(publishedMetadata -> new CommandResult(command, result.events(), publishedMetadata))));
        } else {
            return entitySnapshotProvider.snapshotFor(metadata, command, entity)
                    .thenCompose(snapshot -> {
                        if (snapshot.lastUpdatedOn() > metadata.getTimestamp())
                        {
                            return CompletableFuture.failedStage(new EntityWrongVersionException(Long.toString(snapshot.lastUpdatedOn()), Long.toString(metadata.getTimestamp()), entity.getClass().getSimpleName(), command.id()));
                        } else
                        {
                            return CompletableFuture.completedStage(snapshot.state());
                        }
                    })
                    .thenCompose(snapshot -> entity.handle(metadata, snapshot, command))
                    .thenCompose(result -> domainEventPublisher.publish(new MetadataEvents(result.metadata(), result.events()))
                            .thenApply(publishedMetadata -> new CommandResult(command, result.events(), publishedMetadata)));
        }
    }
}
