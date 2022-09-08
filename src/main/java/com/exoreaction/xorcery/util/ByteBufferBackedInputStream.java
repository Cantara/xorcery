package com.exoreaction.xorcery.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * ByteBuffer backed inputstream that supports mark/reset/skip.
 */
public class ByteBufferBackedInputStream extends InputStream {
    protected final ByteBuffer byteBuffer;

    public ByteBufferBackedInputStream(ByteBuffer buf) {
        byteBuffer = buf;
    }

    @Override
    public int available() {
        return byteBuffer.remaining();
    }

    @Override
    public int read() throws IOException {
        return byteBuffer.hasRemaining() ? (byteBuffer.get() & 0xFF) : -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (!byteBuffer.hasRemaining())
        {
            return -1;
        }

        len = Math.min(len, byteBuffer.remaining());
        byteBuffer.get(bytes, off, len);
        return len;
    }

    @Override
    public synchronized void mark(int readlimit) {
        byteBuffer.mark();
    }

    @Override
    public synchronized void reset() throws IOException {
        byteBuffer.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        byteBuffer.position(byteBuffer.position() + (int) n);
        return n;
    }
}