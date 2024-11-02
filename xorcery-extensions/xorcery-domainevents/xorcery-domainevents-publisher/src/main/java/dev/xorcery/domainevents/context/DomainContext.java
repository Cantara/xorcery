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
package dev.xorcery.domainevents.context;

import dev.xorcery.domainevents.entity.Command;
import jakarta.ws.rs.NotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface DomainContext {

    default List<Command> commands() {
        return Collections.emptyList();
    }

    default <T extends Command> Optional<T> command(Class<T> commandClass) {
        return commands().stream()
                .filter(commandClass::isInstance)
                .map(commandClass::cast)
                .findFirst();
    }

    default <T extends Command> CompletableFuture<CommandResult<T>> handle(CommandMetadata metadata, T command) {
        return CompletableFuture.failedFuture(new NotFoundException());
    }
}
