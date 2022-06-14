package com.exoreaction.reactiveservices.disruptor;

import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class Event<T>
{
    public Metadata metadata;
    public T event;
}
