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

    public static final String SERVICE_TYPE = "eventstore";
    private final EventStoreDBClient client;
    private final EventStoreDBProjectionManagementClient projectionManagementClient;
    private final ObjectMapper objectMapper;
    private ServiceResourceObject sro;

    @Inject
    public EventStoreService(ServiceResourceObjects serviceResourceObjects,
                             Configuration configuration,
                             Provider<ReactiveStreamsServer> reactiveStreams) throws ParseError {
        ServiceResourceObject.Builder builder = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .api(EventStoreRels.eventstore.name(), "api/eventstore");

        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        client = EventStoreDBClient.create(settings);

        // Test connection
        StreamMetadata metadata = client.getStreamMetadata("$all").join();
        logger.info("$all stream metadata:" + metadata.toString());

        projectionManagementClient = EventStoreDBProjectionManagementClient.create(settings);

        // Read
        if (configuration.getBoolean("eventstore.publisher.enabled").orElse(true)) {
            builder.publisher("eventstore");
            reactiveStreams.get().publisher("eventstore", cfg -> new EventStorePublisher(client, objectMapper, cfg), EventStorePublisher.class);
        }

        // Write
        if (configuration.getBoolean("eventstore.subscriber.enabled").orElse(true)) {
            builder.subscriber("eventstore");
            reactiveStreams.get().subscriber("eventstore", cfg -> new EventStoreSubscriber(client, cfg), EventStoreSubscriber.class);
        }

        sro = builder.build();
        serviceResourceObjects.add(sro);
    }

    public CompletionStage<StreamModel> getStream(String id) {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, 1, ReadStreamOptions.get().backwards().fromEnd()),
                (streamMetaData, readResult) ->
                {

                    return new StreamModel(id, readResult.getEvents().stream().findFirst().map(event -> event.getEvent().getStreamRevision().getValueUnsigned()).orElse(-1L));
                });
    }

    public EventStoreDBClient getClient() {
        return client;
    }

    public EventStoreDBProjectionManagementClient getProjectionManagementClient() {
        return projectionManagementClient;
    }
}
