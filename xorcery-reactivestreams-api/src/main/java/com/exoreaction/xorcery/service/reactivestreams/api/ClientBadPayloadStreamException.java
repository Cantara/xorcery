package com.exoreaction.xorcery.service.reactivestreams.api;

/**
 * Stream exception indicating that the client did something wrong, typically with configuration or authentication problems.
 */
public class ClientBadPayloadStreamException
    extends ClientStreamException
{
    public ClientBadPayloadStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientBadPayloadStreamException(String message) {
        super(message);
    }
}
