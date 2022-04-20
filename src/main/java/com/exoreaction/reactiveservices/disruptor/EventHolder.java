package com.exoreaction.reactiveservices.disruptor;

import org.eclipse.jetty.util.MultiMap;

import java.nio.ByteBuffer;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class EventHolder<T>
{
    // Serialized event
    public ByteBuffer headers;
    public ByteBuffer body;

    // Deserialized event
    public MultiMap<String> metadata = new MultiMap<>();
    public T event;
}
