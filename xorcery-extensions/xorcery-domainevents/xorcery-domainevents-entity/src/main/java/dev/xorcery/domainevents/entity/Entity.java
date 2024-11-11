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

import dev.xorcery.collections.Element;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.command.Command;
import dev.xorcery.domainevents.context.CommandMetadata;
import dev.xorcery.domainevents.context.CommandResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Entity {

    private static final Map<Class<?>, Map<Class<?>, Method>> handleMethods = new ConcurrentHashMap<>();

    private List<DomainEvent> events = null;

    protected CommandMetadata metadata;
    protected Command command;
    protected Element snapshot;

    public String id()
    {
        return command.id();
    }

    protected void before(Command command)
            throws Exception {
    }

    protected void after(Command command)
            throws Exception {
    }

    public CompletableFuture<CommandResult> handle(CommandMetadata metadata, Element snapshot, Command command) {
        this.metadata = metadata;
        this.command = command;
        this.snapshot = snapshot;

        // Handle command
        try {
            Method handleMethod = handleMethods.computeIfAbsent(getClass(), this::calculateCommandHandlers).get(command.getClass());

            if (handleMethod == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("No handle method for command type " +
                        command.getClass().getName()));
            }

            before(command);
            handleMethod.invoke(this, command);
            after(command);
        } catch (InvocationTargetException e) {
            return CompletableFuture.failedFuture(e.getTargetException());
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }

        try {
            return CompletableFuture.completedFuture(new CommandResult(command, events != null ? events : Collections.emptyList(), metadata.context()));
        } finally {
            events = null;
        }
    }

    protected void add(DomainEvent event) {
        if (event != null) {
            if (events == null)
            {
                events = new ArrayList<>();
            }
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
