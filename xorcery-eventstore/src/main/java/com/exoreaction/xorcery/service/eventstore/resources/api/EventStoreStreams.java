package com.exoreaction.xorcery.service.eventstore.resources.api;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadStreamOptions;
import com.exoreaction.xorcery.service.eventstore.EventStoreService;
import com.exoreaction.xorcery.service.eventstore.model.StreamModel;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionStage;

@Service
public class EventStoreStreams {

    private final EventStoreDBClient client;

    @Inject
    public EventStoreStreams(EventStoreService eventStoreService) {
        client = eventStoreService.getClient();
    }

    public CompletionStage<StreamModel> getStream(String id) {
        return client.getStreamMetadata(id).thenCombine(client.readStream(id, ReadStreamOptions.get().maxCount(1).backwards().fromEnd()),
                (streamMetaData, readResult) ->
                {
                    return new StreamModel(id, readResult.getEvents().stream().findFirst().map(event -> event.getEvent().getRevision()).orElse(-1L));
                });
    }
}
