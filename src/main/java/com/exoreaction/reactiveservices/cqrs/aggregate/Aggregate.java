package com.exoreaction.reactiveservices.cqrs.aggregate;

import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Aggregate<SNAPSHOT extends AggregateSnapshot> {

    private static final Map<Class<?>, Map<Class<?>, Method>> handleMethods = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<Class<?>, Method>> applyMethods = new ConcurrentHashMap<>();

    private final List<DomainEvent> changes = new ArrayList<>();

    private Metadata metadata;

    public abstract SNAPSHOT getSnapshot();

    protected void setSnapshot(SNAPSHOT snapshot) {
        throw new IllegalArgumentException("Unknown snapshot");
    }

    protected void before(Command command)
            throws Exception
    {
    }

    protected void after(Command command)
            throws Exception
    {
    }

    public DomainEvents handle(Metadata metadata, SNAPSHOT snapshot, Command command)
            throws Throwable {
        this.metadata = metadata;
        setSnapshot(snapshot);

        Method handleMethod = handleMethods.computeIfAbsent(getClass(), clazz ->
        {
            Map<Class<?>, Method> aggregateCommandMethods = new HashMap<>();
            for (Method method : getClass().getDeclaredMethods()) {
                if (method.getName().equals("handle") &&
                        Command.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    aggregateCommandMethods.put(method.getParameterTypes()[0], method);
                }
            }
            return aggregateCommandMethods;
        }).get(command.getClass());

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
                throw iae;
            }

            throw new IllegalArgumentException(e.getCause().getMessage(), e.getCause());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return new DomainEvents(changes);
    }

    protected void add(DomainEvent event) {
        if (event != null) {
            applyEvent(event);
            changes.add(event);
        }
    }

    private void applyEvent(DomainEvent event) {
        Method applyMethod = applyMethods.computeIfAbsent(getClass(), clazz ->
        {
            Map<Class<?>, Method> aggregateEventMethods = new HashMap<>();
            for (Method method : getClass().getDeclaredMethods()) {
                if (method.getName().equals("apply") &&
                        DomainEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    method.setAccessible(true);
                    aggregateEventMethods.put(method.getParameterTypes()[0], method);
                }
            }
            return aggregateEventMethods;
        }).get(event.getClass());

        // Apply event
        if (applyMethod != null) {
            try {
                applyMethod.invoke(this, event);
            } catch (InvocationTargetException e) {
                if (e.getTargetException() instanceof IllegalArgumentException) {
                    throw (IllegalArgumentException) e.getTargetException();
                }

                throw new IllegalArgumentException(e.getCause().getMessage(), e.getCause());
            } catch (Exception e) {
                // Ignore, aggregate doesn't care about this event for snapshot purposes
            }
        }
    }
}
