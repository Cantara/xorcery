package dev.xorcery.reactivestreams.server;

import org.eclipse.jetty.io.RetainableByteBuffer;
import org.eclipse.jetty.websocket.api.Callback;

public record ReleaseCallback(RetainableByteBuffer byteBuffer)
        implements Callback {
    @Override
    public void succeed() {
        byteBuffer.release();
    }

    @Override
    public void fail(Throwable x) {
        byteBuffer.release();
    }
}
