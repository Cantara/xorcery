package com.exoreaction.xorcery.collections;

import java.util.Map;
import java.util.Optional;

public interface MapElement<K,V>
    extends Element
{
    Map<K,V> map();

    @Override
    default Optional<Object> get(String name) {
        return Optional.ofNullable(map().get(name));
    }
}
