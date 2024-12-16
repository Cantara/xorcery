package dev.xorcery.neo4j.client;

import dev.xorcery.collections.Element;
import org.neo4j.graphdb.Entity;

import java.util.Optional;

public interface EntityElement
        extends Element
{
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
