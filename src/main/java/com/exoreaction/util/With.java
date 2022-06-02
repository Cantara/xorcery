package com.exoreaction.util;

import java.util.function.Consumer;

/**
 * Builders can implement this to make it easy to use visitor pattern.
 *
 * @param <T>
 */
public interface With<T> {

    default T with( Consumer<T>... consumers )
    {
        for ( Consumer<T> consumer : consumers )
        {
            consumer.accept( (T)this );
        }
        return (T)this;
    }

}
