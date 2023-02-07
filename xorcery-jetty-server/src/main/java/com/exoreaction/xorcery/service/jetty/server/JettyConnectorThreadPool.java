package com.exoreaction.xorcery.service.jetty.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */
public final class JettyConnectorThreadPool extends QueuedThreadPool {
    @Override
    public Thread newThread(Runnable runnable) {
        var thread = super.newThread(runnable);
        thread.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
        return thread;
    }

    public static final class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private static final Logger logger = LogManager.getLogger(JettyConnectorThreadPool.class);

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.warn("Unhandled exception detected on thread " + t.getName(), e);
        }
    }
}
