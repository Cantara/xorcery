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
package dev.xorcery.test.context;

import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import dev.xorcery.domainevents.context.DomainContext;
import dev.xorcery.domainevents.publisher.api.CommandHandler;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record ThingContext(String id, ThingModel model, ServiceLocator thingSupplier, CommandHandler commandHandler)
        implements DomainContext {

    @Service
    public static class Factory {
        private final ServiceLocator thingSupplier;
        private final CommandHandler commandHandler;

        @Inject
        public Factory(ServiceLocator thingSupplier, CommandHandler commandHandler) {
            this.thingSupplier = thingSupplier;
            this.commandHandler = commandHandler;
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
