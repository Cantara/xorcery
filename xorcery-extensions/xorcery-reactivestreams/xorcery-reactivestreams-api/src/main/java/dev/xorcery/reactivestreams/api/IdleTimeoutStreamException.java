package dev.xorcery.reactivestreams.api;

public class IdleTimeoutStreamException
    extends StreamException
{
    public IdleTimeoutStreamException() {
        super(1001, "Connection Idle Timeout");
    }
}
