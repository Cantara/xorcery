package com.exoreaction.xorcery.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Enums {

    private static final Map<Enum<?>, String> fieldMappings = new ConcurrentHashMap<>();

    /**
     * Map an enum Some.name to "some_name"
     *
     * @param anEnum enum value
     * @return stringified enum
     */
    public static String toField(Enum<?> anEnum) {
        return fieldMappings.computeIfAbsent(anEnum, e ->
                e.getDeclaringClass().getSimpleName().toLowerCase() + "_" + e.name());
    }
}
