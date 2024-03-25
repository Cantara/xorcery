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
package com.exoreaction.xorcery.jetty.server;


import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

@Service
@Priority(0)
public class Log4j2HandlerFactory
        implements Factory<Log4j2ThreadContextHandler> {
    private final Log4j2ThreadContextHandler log4J2ThreadContextHandler;

    @Inject
    public Log4j2HandlerFactory(Configuration configuration) {
        log4J2ThreadContextHandler = new Log4j2ThreadContextHandler(configuration);
    }

    @Override
    @Named("jetty.server.log4j2")
    @Singleton
    public Log4j2ThreadContextHandler provide() {
        return log4J2ThreadContextHandler;
    }

    @Override
    public void dispose(Log4j2ThreadContextHandler instance) {
    }
}
