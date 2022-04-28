package com.exoreaction.reactiveservices.disruptor;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */
public class Event<T>
{
    // Serialized event
    public ByteBuffer headers;
    public ByteBuffer body;

    // Deserialized event
    public Metadata metadata = new Metadata();
    public T event;

    // Optional result
    public CompletableFuture<Metadata> result;
}
