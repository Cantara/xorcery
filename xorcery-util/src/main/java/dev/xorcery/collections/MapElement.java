package dev.xorcery.collections;

import java.util.Map;
import java.util.Optional;

public interface MapElement<K,V>
    extends Element
{
    static <K,V> MapElement<K,V> element(Map<K,V> map)
    {
        return () -> map;
    }

    Map<K,V> map();

    @Override
    default <T> Optional<T> get(String name) {
        return Optional.ofNullable((T)map().get(name));
    }
}
