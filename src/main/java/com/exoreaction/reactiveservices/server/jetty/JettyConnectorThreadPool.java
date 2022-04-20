package com.exoreaction.reactiveservices.server.jetty;

import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.process.JerseyProcessingUncaughtExceptionHandler;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */
public final class JettyConnectorThreadPool extends QueuedThreadPool
{
    @Override
    public Thread newThread( Runnable runnable )
    {
        var thread = super.newThread( runnable );
        thread.setUncaughtExceptionHandler( new JerseyProcessingUncaughtExceptionHandler() );
        return thread;
    }
}
