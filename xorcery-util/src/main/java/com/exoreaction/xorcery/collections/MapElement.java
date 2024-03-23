package com.exoreaction.xorcery.collections;

import java.util.Map;
import java.util.Optional;

public record MapElement(Map<String, Object> map)
    implements Element
{
    @Override
    public Optional<Object> get(String name) {
        return Optional.ofNullable(map.get(name));
    }
}
