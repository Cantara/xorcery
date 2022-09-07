package com.exoreaction.xorcery.service.reactivestreams.api;

import com.exoreaction.xorcery.cqrs.metadata.Metadata;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class WithMetadata<T>
{
    private Metadata metadata;
    private T event;

    public WithMetadata() {
    }

    public WithMetadata(Metadata metadata, T event) {
        this.metadata = metadata;
        this.event = event;
    }

    public void set(Metadata metadata, T event)
    {
        this.metadata = metadata;
        this.event = event;
    }

    public void set(WithMetadata<T> other)
    {
        this.metadata = other.metadata;
        this.event = other.event;
    }

    public Metadata metadata()
    {
        return metadata;
    }

    public T event()
    {
        return event;
    }
}
