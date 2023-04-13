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
