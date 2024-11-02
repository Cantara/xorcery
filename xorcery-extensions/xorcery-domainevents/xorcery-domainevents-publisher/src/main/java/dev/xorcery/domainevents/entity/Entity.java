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
package dev.xorcery.domainevents.entity;

import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;
import jakarta.validation.ValidationException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Entity<SNAPSHOT> {

    private static final Map<Class<?>, Map<Class<?>, Method>> handleMethods = new ConcurrentHashMap<>();

    private final List<DomainEvent> events = new ArrayList<>();

    protected CommandMetadata metadata;
    protected SNAPSHOT snapshot;

    protected void before(Command command)
            throws Exception {
    }

    protected void after(Command command)
            throws Exception {
    }

    public <T extends Command> CompletableFuture<CommandResult<T>> handle(CommandMetadata metadata, SNAPSHOT snapshot, T command) {
        this.metadata = metadata;
        this.snapshot = snapshot;

        Method handleMethod = handleMethods.computeIfAbsent(getClass(), this::calculateCommandHandlers).get(command.getClass());

        if (handleMethod == null) {
            throw new IllegalArgumentException("No handle method for command type " +
                    command.getClass().getName());
        }

        // Handle command
        try {
            before(command);
            handleMethod.invoke(this, command);
            after(command);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IllegalArgumentException iae) {
                return CompletableFuture.failedFuture(iae);
            } else if (e.getTargetException() instanceof ValidationException ve) {
                return CompletableFuture.failedFuture(ve);
            }

            return CompletableFuture.failedFuture(new IllegalArgumentException(e.getCause().getMessage(), e.getCause()));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new IllegalArgumentException(e));
        }

        return CompletableFuture.completedFuture(new CommandResult<>(command, events, metadata.context()));
    }

    protected void add(DomainEvent event) {
        if (event != null) {
            events.add(event);
        }
    }

    private Map<Class<?>, Method> calculateCommandHandlers(Class<?> clazz) {
        Map<Class<?>, Method> commandHandlerMethods = new HashMap<>();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals("handle") &&
                    Command.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                commandHandlerMethods.put(method.getParameterTypes()[0], method);
            }
        }
        return commandHandlerMethods;
    }
}
