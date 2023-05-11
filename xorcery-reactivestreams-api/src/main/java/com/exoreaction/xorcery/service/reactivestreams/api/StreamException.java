package com.exoreaction.xorcery.service.reactivestreams.api;

import java.io.IOException;

/**
 * All exceptions reported to subscribers will be wrapped in a subclass of this exception.
 */
public class StreamException
    extends IOException
{
    public StreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamException(String message) {
        super(message);
    }
}
