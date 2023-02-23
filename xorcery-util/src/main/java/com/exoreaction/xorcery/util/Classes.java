package com.exoreaction.xorcery.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Classes {

    public static Class<Object> getClass(Type type)
    {
        return (Class<Object>)(type instanceof ParameterizedType ? ((ParameterizedType) type).getRawType() : type);
    }
}
