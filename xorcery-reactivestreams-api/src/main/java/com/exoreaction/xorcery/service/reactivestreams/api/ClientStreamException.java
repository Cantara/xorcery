package com.exoreaction.xorcery.service.reactivestreams.api;

/**
 * Stream exception indicating that the client did something wrong, typically with configuration or authentication problems.
 */
public class ClientStreamException
    extends StreamException
{
    public ClientStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientStreamException(String message) {
        super(message);
    }
}
