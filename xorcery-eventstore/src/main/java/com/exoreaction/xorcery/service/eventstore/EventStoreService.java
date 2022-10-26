package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.eventstore.model.StreamModel;
import com.exoreaction.xorcery.service.eventstore.streams.EventStorePublisher;
import com.exoreaction.xorcery.service.eventstore.streams.EventStoreSubscriber;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionStage;

@Service
@Named("eventstore")
public class EventStoreService {

    private static final Logger logger = LogManager.getLogger(EventStoreService.class);

    public static final String SERVICE_TYPE = "eventstore";
    private final EventStoreDBClient client;
    private final ObjectMapper objectMapper;
    private ServiceResourceObject sro;

    @Inject
    public EventStoreService(Topic<ServiceResourceObject> registryTopic,
                             Configuration configuration,
                             ReactiveStreams reactiveStreams) throws ParseError {
        this.sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .api(EventStoreRels.eventstore.name(), "api/eventstore")
                .websocket(EventStoreRels.eventpublisher.name(), "ws/eventstore/publisher")
                .websocket(EventStoreRels.eventsubscriber.name(), "ws/eventstore/subscriber")
                .build();

        this.objectMapper = new ObjectMapper()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        client = EventStoreDBClient.create(settings);

        // Test connection
        StreamMetadata metadata = client.getStreamMetadata("$all").join();
        logger.info("$all stream metadata:" + metadata.toString());

        // Read
        sro.getLinkByRel(EventStoreRels.eventpublisher.name()).ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new EventStorePublisher(client, objectMapper, cfg), EventStorePublisher.class);
        });

        // Write
        sro.getLinkByRel(EventStoreRels.eventsubscriber.name()).ifPresent(link ->
        {
            reactiveStreams.subscriber(link.getHrefAsUri().getPath(), cfg -> new EventStoreSubscriber(client, cfg), EventStoreSubscriber.class);
        });

        registryTopic.publish(sro);
    }

    public CompletionStage<StreamModel> getStream(String id) {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, 1, ReadStreamOptions.get().backwards().fromEnd()),
                (streamMetaData, readResult) ->
                {

                    return new StreamModel(id, readResult.getEvents().stream().findFirst().map(event -> event.getEvent().getStreamRevision().getValueUnsigned()).orElse(-1L));
                });
    }
}
