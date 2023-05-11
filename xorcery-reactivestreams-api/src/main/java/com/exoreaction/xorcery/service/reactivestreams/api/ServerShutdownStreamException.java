package com.exoreaction.xorcery.service.reactivestreams.api;


/**
 * Stream exception indicating that the server is shutting down.
 */
public class ServerShutdownStreamException
    extends ServerStreamException
{
    public ServerShutdownStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerShutdownStreamException(String message) {
        super(message);
    }
}
