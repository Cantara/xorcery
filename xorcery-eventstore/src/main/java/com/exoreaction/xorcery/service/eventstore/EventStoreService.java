package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.eventstore.model.StreamModel;
import com.exoreaction.xorcery.service.eventstore.streams.EventStorePublisher;
import com.exoreaction.xorcery.service.eventstore.streams.EventStoreSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionStage;

@Service
@Named("eventstore")
public class EventStoreService {

    private static final Logger logger = LogManager.getLogger(EventStoreService.class);

    private final EventStoreDBClient client;
    private final EventStoreDBClientSettings settings;

    @Inject
    public EventStoreService(Configuration configuration) throws ConnectionStringParsingException {

        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        settings = EventStoreDBConnectionString.parse(connectionString);
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
