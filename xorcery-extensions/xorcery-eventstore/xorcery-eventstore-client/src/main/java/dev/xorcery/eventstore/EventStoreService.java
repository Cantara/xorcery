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
package dev.xorcery.eventstore;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.StreamMetadata;
import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service(name="eventstore")
public class EventStoreService {

    private static final Logger logger = LogManager.getLogger(EventStoreService.class);

    private final EventStoreDBClient client;
    private final EventStoreDBClientSettings settings;

    @Inject
    public EventStoreService(Configuration configuration) {

        EventStoreConfiguration eventStoreConfiguration = new EventStoreConfiguration(configuration.getConfiguration("eventstore"));
        settings = EventStoreDBConnectionString.parseOrThrow(eventStoreConfiguration.getURI());
        client = EventStoreDBClient.create(settings);

        // Test connection
        logger.info("Testing connection to "+eventStoreConfiguration.getURI());
        ConnectionTestProcess connectionTestProcess = new ConnectionTestProcess(client, new CompletableFuture<>());
        connectionTestProcess.start();
        StreamMetadata metadata = connectionTestProcess.result().orTimeout(60, TimeUnit.SECONDS).join();
        logger.info("Connection ok");
    }

    public EventStoreDBClient getClient() {
        return client;
    }

    public EventStoreDBClientSettings getSettings() {
        return settings;
    }
}
