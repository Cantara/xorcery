/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
