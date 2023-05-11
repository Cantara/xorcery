package com.exoreaction.xorcery.service.reactivestreams.api;


/**
 * Stream exception indicating that the client is shutting down.
 */
public class ClientShutdownStreamException
    extends ClientStreamException
{
    public ClientShutdownStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientShutdownStreamException(String message) {
        super(message);
    }
}
