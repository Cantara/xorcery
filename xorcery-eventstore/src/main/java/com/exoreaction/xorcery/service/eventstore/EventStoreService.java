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
package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

@Service
@Named("eventstore")
public class EventStoreService {

    private static final Logger logger = LogManager.getLogger(EventStoreService.class);

    private final EventStoreDBClient client;
    private final EventStoreDBClientSettings settings;

    @Inject
    public EventStoreService(Configuration configuration) throws ConnectionStringParsingException {

        EventStoreConfiguration eventStoreConfiguration = new EventStoreConfiguration(configuration.getConfiguration("eventstore"));
        settings = EventStoreDBConnectionString.parse(eventStoreConfiguration.getURL());
        client = EventStoreDBClient.create(settings);

        // Test connection
        logger.info("Testing connection");
        StreamMetadata metadata = client.getStreamMetadata("$all").join();
        logger.info("Connection ok");
    }

    public EventStoreDBClient getClient() {
        return client;
    }

    public EventStoreDBClientSettings getSettings() {
        return settings;
    }
}
