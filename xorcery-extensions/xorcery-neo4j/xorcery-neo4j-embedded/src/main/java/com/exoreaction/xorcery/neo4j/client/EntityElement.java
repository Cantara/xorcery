package com.exoreaction.xorcery.neo4j.client;

import com.exoreaction.xorcery.collections.Element;
import org.neo4j.graphdb.Entity;

import java.util.Optional;
import java.util.function.Supplier;

public interface EntityElement
        extends Element
{
    static Supplier<RuntimeException> missing(Enum<?> name) {
        return () -> new IllegalArgumentException("Missing property '" + name + "'");
    }

    Entity entity();

    @Override
    default <T> Optional<T> get(String name) {
        return Optional.ofNullable((T)entity().getProperty(name, null));
    }

    @Override
    default boolean has(String name) {
        return entity().hasProperty(name);
    }
}
