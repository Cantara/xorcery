package com.exoreaction.xorcery.disruptor;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class Event<T>
{
    public Metadata metadata;
    public T event;
}
