package com.exoreaction.xorcery.reactivestreams.client.reactor;

import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

public class ByteBufferReader<OUTPUT>
    implements java.util.function.Function<ByteBuffer,OUTPUT>
{
    private final MessageReader<OUTPUT> messageReader;

    public ByteBufferReader(MessageReader<OUTPUT> messageReader) {
        this.messageReader = messageReader;
    }

    @Override
    public OUTPUT apply(ByteBuffer byteBuffer) {
        try {
            return messageReader.readFrom(byteBuffer.array(), byteBuffer.arrayOffset(), byteBuffer.limit());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
