package com.exoreaction.util.function;

import java.util.function.Function;

/**
 * Function that takes two functions. If the first function returns null, then use the second one on apply.
 *
 * @param <T>
 * @param <R>
 */
public class FallbackFunction<T,R>
    implements Function<T,R>
{
    final private Function<T,R> function;
    final private Function<T,R> fallback;

    public FallbackFunction(Function<T, R> function, Function<T, R> fallback) {
        this.function = function;
        this.fallback = fallback;
    }

    @Override
    public R apply(T from) {
        R value = function.apply(from);
        return value == null ? fallback.apply(from) : value;
    }
}
