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
package dev.xorcery.kurrent.client.api;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static dev.xorcery.configuration.Configuration.missing;

@Service(name = "kurrent")
@RunLevel(4)
public class KurrentClients
        implements PreDestroy {
    private final KurrentConfiguration kurrentConfiguration;
    private final OpenTelemetry openTelemetry;
    private final Logger logger;
    private final LoggerContext loggerContext;
    private final Map<String, KurrentClient> clients = new ConcurrentHashMap<>();

    @Inject
    public KurrentClients(Configuration configuration, OpenTelemetry openTelemetry, LoggerContext loggerContext) {
        this.kurrentConfiguration = new KurrentConfiguration(configuration.getConfiguration("kurrent"));
        this.openTelemetry = openTelemetry;
        this.logger = loggerContext.getLogger(getClass());
        this.loggerContext = loggerContext;
    }

    public KurrentClient getDefaultClient() {
        return getClient("default");
    }

    public KurrentClient getClient(String name) {
        return clients.computeIfAbsent(name, this::newClient);
    }

    private KurrentClient newClient(String name) {
        return kurrentConfiguration.getClient(name)
                .map(configuration -> new KurrentClient(configuration, loggerContext, openTelemetry))
                .orElseThrow(missing(name));
    }

    @Override
    public void preDestroy() {
        for (KurrentClient value : clients.values()) {
            try {
                value.getClient().shutdown().orTimeout(10, TimeUnit.SECONDS).join();
            } catch (Throwable e) {
                logger.warn("Could not close Kurrent client", e);
            }
        }
    }
}
