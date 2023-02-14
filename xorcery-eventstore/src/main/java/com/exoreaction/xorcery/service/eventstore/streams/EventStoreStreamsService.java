package com.exoreaction.xorcery.service.eventstore.streams;


import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.eventstore.EventStoreRels;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import com.exoreaction.xorcery.service.eventstore.model.StreamModel;
import com.exoreaction.xorcery.service.eventstore.resources.api.EventStoreStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionStage;

@Service
@RunLevel(6)
public class EventStoreStreamsService {

    @Inject
    public EventStoreStreamsService(EventStoreService eventStoreService,
                                    ServiceResourceObjects serviceResourceObjects,
                                    Configuration configuration,
                                    ReactiveStreamsServer reactiveStreams) {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        ServiceResourceObject.Builder builder = new ServiceResourceObject.Builder(() -> configuration, "eventstore")
                .api(EventStoreRels.eventstore.name(), "api/eventstore");

        EventStoreDBClient client = eventStoreService.getClient();

        // Read
        if (configuration.getBoolean("eventstore.streams.publisher.enabled").orElse(true)) {
            builder.publisher("eventstore");
            reactiveStreams.publisher("eventstore", cfg -> new EventStorePublisher(client, objectMapper, cfg), EventStorePublisher.class);
        }

        // Write
        if (configuration.getBoolean("eventstore.streams.subscriber.enabled").orElse(true)) {
            builder.subscriber("eventstore");
            reactiveStreams.subscriber("eventstore", cfg -> new EventStoreSubscriber(client, cfg), EventStoreSubscriber.class);
        }

        ServiceResourceObject sro = builder.build();
        serviceResourceObjects.add(sro);
    }
}
