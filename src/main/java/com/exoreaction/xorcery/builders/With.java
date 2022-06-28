package com.exoreaction.xorcery.builders;

import java.util.function.Consumer;

/**
 * Builders can implement this to make it easy to use visitor pattern. This avoids breaking the DSL flow use in many cases.
 *
 * @param <T>
 */
public interface With<T> {

    @SuppressWarnings("unchecked")
    default T with( Consumer<T>... consumers )
    {
        for ( Consumer<T> consumer : consumers )
        {
            consumer.accept( (T)this );
        }
        return (T)this;
    }
}
