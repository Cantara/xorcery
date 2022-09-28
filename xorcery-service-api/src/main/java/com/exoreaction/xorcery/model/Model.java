package com.exoreaction.xorcery.model;

import com.exoreaction.xorcery.json.JsonElement;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface Model
    extends JsonElement
{
    Map<Enum<?>,String> fieldMappings = new ConcurrentHashMap<>();

    default Optional<String> getString(Enum<?> name) {
        return getString(toField(name));
    }

    default Optional<Integer> getInteger(Enum<?> name) {
        return getInteger(toField(name));
    }

    default Optional<Long> getLong(Enum<?> name) {
        return getLong(toField(name));
    }

    default Optional<Boolean> getBoolean(Enum<?> name) {
        return getBoolean(toField(name));
    }

    default <T extends Enum<T>> Optional<T> getEnum(Enum<?> name, Class<T> enumClass) {
        return getEnum(toField(name), enumClass);
    }

    default String toField( Enum<?> anEnum )
    {
        return fieldMappings.computeIfAbsent( anEnum, e ->
                e.getDeclaringClass().getSimpleName().toLowerCase() + "_" + e.name() );
    }
}
