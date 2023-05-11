package com.exoreaction.xorcery.service.reactivestreams.api;


/**
 * Stream exception indicating that the server did something wrong.
 */
public class ServerStreamException
    extends StreamException
{
    public ServerStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerStreamException(String message) {
        super(message);
    }
}
