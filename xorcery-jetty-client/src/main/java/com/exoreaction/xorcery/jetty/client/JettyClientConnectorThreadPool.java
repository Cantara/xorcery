package com.exoreaction.xorcery.jetty.client;

import io.opentelemetry.context.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public final class JettyClientConnectorThreadPool extends QueuedThreadPool {
    @Override
    public Thread newThread(Runnable runnable) {
        var thread = super.newThread(runnable);
        thread.setUncaughtExceptionHandler(new LoggingUncaughtExceptionHandler());
        return thread;
    }

    @Override
    public void execute(Runnable job) {
        super.execute(Context.current().wrap(job));
    }

    @Override
    public boolean tryExecute(Runnable task) {
        return super.tryExecute(Context.current().wrap(task));
    }

    public static final class LoggingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private static final Logger logger = LogManager.getLogger(JettyClientConnectorThreadPool.class);

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            logger.warn("Unhandled exception detected on thread " + t.getName(), e);
        }
    }
}
