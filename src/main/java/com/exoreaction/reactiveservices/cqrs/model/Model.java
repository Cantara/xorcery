package com.exoreaction.reactiveservices.cqrs.model;

import com.exoreaction.reactiveservices.json.JsonElement;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public interface Model
    extends JsonElement
{
    Map<Enum<?>,String> fieldMappings = new ConcurrentHashMap<>();

    default String getString(Enum<?> name) {
        return getString(toField(name));
    }

    default Optional<String> getOptionalString(Enum<?> name) {
        return getOptionalString(toField(name));
    }

    default int getInt(Enum<?> name) {
        return getInt(toField(name));
    }

    default Optional<Integer> getOptionalInt(Enum<?> name) {
        return getOptionalInt(toField(name));
    }

    default long getLong(Enum<?> name) {
        return getLong(toField(name));
    }

    default Optional<Long> getOptionalLong(Enum<?> name) {
        return getOptionalLong(toField(name));
    }

    default boolean getBoolean(Enum<?> name) {
        return getBoolean(toField(name));
    }

    default Optional<Boolean> getOptionalBoolean(Enum<?> name) {
        return getOptionalBoolean(toField(name));
    }

    default <T extends Enum<T>> T getEnum(Enum<?> name, Class<T> enumClass) {
        return getEnum(toField(name), enumClass);
    }

    default <T extends Enum<T>> Optional<T> getOptionalEnum(Enum<?> name, Class<T> enumClass) {
        return getOptionalEnum(toField(name), enumClass);
    }
    default String toField( Enum<?> anEnum )
    {
        return fieldMappings.computeIfAbsent( anEnum, e ->
                e.getDeclaringClass().getSimpleName().toLowerCase() + "_" + e.name() );
    }
}
